package pos.models;

import java.time.LocalDateTime;
import java.util.*;

public class Transaction {
    private String id;
    private LocalDateTime timestamp;
    private User cashier;
    private List<CartItem> items = new ArrayList<>();
    private double subtotal;
    private double discountTotal;
    private double taxTotal;
    private double grandTotal;
    private String receiptPath;
    private Customer customer;
    private String promoCode;
    private boolean refunded;
    

    public Transaction(User cashier) {
        this.id = UUID.randomUUID().toString();
        this.timestamp = LocalDateTime.now();
        this.cashier = cashier;
    }

    public String getId() { return id; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public User getCashier() { return cashier; }
    public List<CartItem> getItems() { return items; }
    public double getSubtotal() { return subtotal; }
    public double getDiscountTotal() { return discountTotal; }
    public double getTaxTotal() { return taxTotal; }
    public double getGrandTotal() { return grandTotal; }
    public String getReceiptPath() { return receiptPath; }
    public Customer getCustomer() { return customer; }
    public String getPromoCode() { return promoCode; }
    public boolean isRefunded() { return refunded; }

    public void setReceiptPath(String path) { this.receiptPath = path; }
    public void setCustomer(Customer c) { this.customer = c; }
    public void setPromoCode(String code) { this.promoCode = code; }
    public void setSubtotal(double v) { this.subtotal = v; }
    public void setDiscountTotal(double v) { this.discountTotal = v; }
    public void setTaxTotal(double v) { this.taxTotal = v; }
    public void setGrandTotal(double v) { this.grandTotal = v; }
    public void setRefunded(boolean r) { this.refunded = r; }
}
