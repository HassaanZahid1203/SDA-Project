package pos.services;

import pos.models.Transaction;
import pos.models.Product;
import pos.models.Payment;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import pos.models.CartItem;

public class ReceiptWriter {
    private final String receiptsDir;

    public ReceiptWriter(String receiptsDir) {
        this.receiptsDir = receiptsDir;
    }

    public String writeReceipt(Transaction tx, List<Payment> payments, double change) throws IOException {
        Path dir = Paths.get(receiptsDir);
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName = "receipt_" + tx.getId() + "_" + timestamp + ".txt";
        Path path = dir.resolve(fileName);

        StringBuilder sb = new StringBuilder();
        sb.append("=== RECEIPT ===\n");
        sb.append("Receipt ID: ").append(tx.getId()).append("\n");
        if (tx.getCashier() != null) {
            sb.append("Cashier: ").append(tx.getCashier().getUsername())
              .append(" (").append(tx.getCashier().getRole()).append(")\n");
        }
        sb.append("Date: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))).append("\n\n");

        sb.append("Items:\n");

        List<CartItem> items = tx.getItems(); 

        if (items.isEmpty()) {
            sb.append("(no items)\n");
        } else {
            for (CartItem ci : items) {
                Product p = ci.getProduct();
                int qty = ci.getQuantity();
                double unit = p.getPrice();

                sb.append(String.format("%s\t%s\tUnit: PKR %.2f\tQty: %d\tSubtotal: PKR %.2f\n",
                        p.getBarcode(),
                        p.getName(),
                        unit,
                        qty,
                        unit * qty));
            }
        }


        sb.append("\nTotals:\n");
        sb.append(String.format("Subtotal: PKR %.2f\n", tx.getSubtotal()));
        sb.append(String.format("Discounts: PKR %.2f\n", tx.getDiscountTotal()));
        sb.append(String.format("Tax: PKR %.2f\n", tx.getTaxTotal()));
        sb.append(String.format("Grand Total: PKR %.2f\n", tx.getGrandTotal()));

        double totalPaid = payments.stream().mapToDouble(Payment::getAmount).sum();
        sb.append("\nPayments:\n");
        for (Payment p : payments) {
            sb.append(String.format("- %s: PKR %.2f\n", p.getMethod(), p.getAmount()));
        }
        sb.append(String.format("Total paid: PKR %.2f\n", totalPaid));
        sb.append(String.format("Change returned: PKR %.2f\n", Math.max(0, change)));

        sb.append("\nThank you for shopping!\n");

        Files.write(path, sb.toString().getBytes());
        return path.toAbsolutePath().toString();
    }
}
