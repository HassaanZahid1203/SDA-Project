package pos.services;

import pos.models.*;

import java.io.IOException;
import java.util.*;

public class POSService {
    private final InventoryManager inventory;
    private final ReceiptWriter receiptWriter;
    private final PromotionService promotionService;
    private final CustomerService customerService;
    private ReportService reportService;

    // In-memory store of transactions keyed by receipt ID for refunds demo
    private final Map<String, Transaction> transactionsById = new HashMap<>();

    public POSService(InventoryManager inventory, ReceiptWriter receiptWriter,
                      PromotionService promotionService, CustomerService customerService,
                      ReportService reportService) {
        this.inventory = inventory;
        this.receiptWriter = receiptWriter;
        this.promotionService = promotionService;
        this.customerService = customerService;
        this.reportService = reportService;
    }

    public void setReportService(ReportService reportService) {
        this.reportService = reportService;
    }


    public Map<String, Transaction> getAllTransactions() {
        return Collections.unmodifiableMap(transactionsById);
    }

    public Transaction startTransaction(User cashier) {
        return new Transaction(cashier);
    }

    public Product findProduct(String query) {
        return inventory.findByQuery(query);
    }

    public boolean addItem(Transaction tx, String barcode, int qty) {
        Product p = inventory.findByBarcode(barcode);
        if (p == null || qty <= 0) return false;
        if (p.getStock() < qty) return false; // restrict out-of-stock
        // Reserve stock immediately (simpler)
        inventory.reserveStock(barcode, qty);

        CartItem ci = new CartItem(p, qty);
        tx.getItems().add(ci);
        return true;
    }

    public void applyPromoCode(Transaction tx, String promoCode) {
        tx.setPromoCode(promoCode);
    }

    public Customer attachOrCreateCustomer(Transaction tx, String phone) {
        Customer c = customerService.attachOrCreate(phone);
        tx.setCustomer(c);
        return c;
    }

    public void recalculateTotals(Transaction tx) {
        double subtotal = tx.getItems().stream().mapToDouble(CartItem::getLineSubtotal).sum();
        double discount = promotionService.applyPromo(tx, tx.getPromoCode(), subtotal);

        double tax = tx.getItems().stream()
                .mapToDouble(ci -> {
                    double lineTax = ci.getProduct().getTaxRate() * (ci.getLineSubtotal() - (discount * (ci.getLineSubtotal() / Math.max(1.0, subtotal))));
                    ci.setLineTax(lineTax);
                    return lineTax;
                }).sum();

        tx.setSubtotal(subtotal);
        tx.setDiscountTotal(discount);
        tx.setTaxTotal(tax);
        tx.setGrandTotal(subtotal - discount + tax);
    }

   
    public boolean completeSale(Transaction tx, List<Payment> payments) {
        // ensure totals are up-to-date
        recalculateTotals(tx);

        double total = tx.getGrandTotal();
        double paid = payments.stream().mapToDouble(Payment::getAmount).sum();
        double epsilon = 0.01;

        if (paid + epsilon < total) {
            // not enough paid
            return false;
        }

        double change = Math.max(0.0, paid - total);

        try {
            // persist inventory changes
            inventory.save();

            // write receipt (ReceiptWriter.writeReceipt handles payments and change)
            String path = receiptWriter.writeReceipt(tx, payments, change);
            tx.setReceiptPath(path);

            // Optionally store transaction for reports
            transactionsById.put(tx.getId(), tx);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


    public boolean refund(String receiptId, User requester) {
        // Only Admin/Manager in UserService, but double-check here if needed
        Transaction tx = transactionsById.get(receiptId);
        if (tx == null || tx.isRefunded()) return false;
        // reverse stock
        tx.getItems().forEach(ci -> inventory.releaseStock(ci.getProduct().getBarcode(), ci.getQuantity()));
        tx.setRefunded(true);
        return true;
    }
}
