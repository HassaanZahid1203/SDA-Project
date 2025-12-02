package pos.models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TransactionCopy {
    private final String id;
    private final LocalDateTime timestamp;
    private final String cashierName;
    private final String cashierRole;
    private final List<CartItem> items;
    private final double subtotal;
    private final double discountTotal;
    private final double taxTotal;
    private final double grandTotal;
    private final String promoCode;
    private final boolean refunded;

    public TransactionCopy(Transaction tx) {
        this.id = tx.getId();
        this.timestamp = tx.getTimestamp();
        this.cashierName = tx.getCashier() != null ? tx.getCashier().getUsername() : "Unknown";
        this.cashierRole = tx.getCashier() != null ? tx.getCashier().getRole() : "Unknown";
        this.items = new ArrayList<>();
        for (CartItem ci : tx.getItems()) {
            this.items.add(new CartItem(ci.getProduct(), ci.getQuantity()));
        }
        this.subtotal = tx.getSubtotal();
        this.discountTotal = tx.getDiscountTotal();
        this.taxTotal = tx.getTaxTotal();
        this.grandTotal = tx.getGrandTotal();
        this.promoCode = tx.getPromoCode();
        this.refunded = tx.isRefunded();
    }

    // Getters
    public String getId() { return id; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getCashierName() { return cashierName; }
    public String getCashierRole() { return cashierRole; }
    public List<CartItem> getItems() { return items; }
    public double getSubtotal() { return subtotal; }
    public double getDiscountTotal() { return discountTotal; }
    public double getTaxTotal() { return taxTotal; }
    public double getGrandTotal() { return grandTotal; }
    public String getPromoCode() { return promoCode; }
    public boolean isRefunded() { return refunded; }
}