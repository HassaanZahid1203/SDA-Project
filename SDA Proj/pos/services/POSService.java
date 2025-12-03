package pos.services;

import pos.models.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class POSService {
    private final InventoryManager inventory;
    private final ReceiptWriter receiptWriter;
    private final PromotionService promotionService;
    private final CustomerService customerService;
    private final TransactionStorage transactionStorage;
    private final RefundLogger refundLogger;
    private ReportService reportService;

    public POSService(InventoryManager inventory, ReceiptWriter receiptWriter,
                      PromotionService promotionService, CustomerService customerService,
                      TransactionStorage transactionStorage) {
        this.inventory = inventory;
        this.receiptWriter = receiptWriter;
        this.promotionService = promotionService;
        this.customerService = customerService;
        this.transactionStorage = transactionStorage;
        this.refundLogger = new RefundLogger("refunds_log.txt");
    }

    public void setReportService(ReportService reportService) {
        this.reportService = reportService;
    }

    public Map<String, Transaction> getAllTransactions() {
        List<Transaction> allTransactions = transactionStorage.loadAllTransactions();
        Map<String, Transaction> map = new HashMap<>();
        for (Transaction tx : allTransactions) {
            map.put(tx.getId(), tx);
        }
        return Collections.unmodifiableMap(map);
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
        if (p.getStock() < qty) return false;
        
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
        recalculateTotals(tx);

        double total = tx.getGrandTotal();
        double paid = payments.stream().mapToDouble(Payment::getAmount).sum();
        double epsilon = 0.01;

        if (paid + epsilon < total) {
            return false;
        }

        double change = Math.max(0.0, paid - total);

        try {
            inventory.save();
            
            String path = receiptWriter.writeReceipt(tx, payments, change);
            tx.setReceiptPath(path);
            
            // Add loyalty points if customer is attached
            if (tx.getCustomer() != null) {
                double loyaltyPoints = tx.getGrandTotal() / 100.0; // 1 point per 100 PKR
                customerService.addLoyaltyPoints(tx.getCustomer().getPhone(), loyaltyPoints);
            }
            
            transactionStorage.saveTransaction(tx);
            
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean refund(String receiptId, User requester) {
        try {
            Transaction tx = transactionStorage.findTransactionById(receiptId);
            if (tx == null || tx.isRefunded()) {
                return false;
            }
            
            // Reverse stock for ALL items
            for (CartItem ci : tx.getItems()) {
                inventory.releaseStock(ci.getProduct().getBarcode(), ci.getQuantity());
            }
            
            // Mark as fully refunded
            tx.setRefunded(true);
            
            // Update in storage
            transactionStorage.updateTransaction(tx);
            
            // Save inventory
            inventory.save();
            
            // Log the refund
            refundLogger.logFullRefund(tx, requester);
            
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public boolean partialRefund(String receiptId, List<Integer> itemIndices, User requester) {
        try {
            Transaction tx = transactionStorage.findTransactionById(receiptId);
            if (tx == null || tx.isRefunded()) {
                return false;
            }
            
            List<CartItem> allItems = tx.getItems();
            List<CartItem> refundedItems = new ArrayList<>();
            double refundAmount = 0;
            
            // Reverse stock for selected items
            for (int index : itemIndices) {
                if (index >= 0 && index < allItems.size()) {
                    CartItem ci = allItems.get(index);
                    inventory.releaseStock(ci.getProduct().getBarcode(), ci.getQuantity());
                    refundedItems.add(ci);
                    refundAmount += ci.getLineSubtotal();
                }
            }
            
            if (refundedItems.isEmpty()) {
                return false;
            }
            
            // Mark items as refunded (we'll create a copy without refunded items)
            List<CartItem> remainingItems = new ArrayList<>();
            for (int i = 0; i < allItems.size(); i++) {
                if (!itemIndices.contains(i)) {
                    remainingItems.add(allItems.get(i));
                }
            }
            
            // Update transaction with remaining items
            // We'll create a new transaction with remaining items
            Transaction updatedTx = new Transaction(tx.getCashier());
            
            // Copy ID and timestamp from original
            try {
                java.lang.reflect.Field idField = Transaction.class.getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(updatedTx, tx.getId());
                
                java.lang.reflect.Field timestampField = Transaction.class.getDeclaredField("timestamp");
                timestampField.setAccessible(true);
                timestampField.set(updatedTx, tx.getTimestamp());
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            // Add remaining items
            updatedTx.getItems().addAll(remainingItems);
            
            // Recalculate totals
            recalculateTotals(updatedTx);
            
            // Copy other properties
            updatedTx.setReceiptPath(tx.getReceiptPath());
            updatedTx.setPromoCode(tx.getPromoCode());
            updatedTx.setRefunded(remainingItems.isEmpty()); // Mark as refunded only if no items left
            
            // Update in storage
            transactionStorage.updateTransaction(updatedTx);
            
            // Save inventory
            inventory.save();
            
            // Log the partial refund
            refundLogger.logPartialRefund(tx, refundedItems, refundAmount, requester);
            
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public Transaction getTransactionById(String id) {
        return transactionStorage.findTransactionById(id);
    }
}
