package pos.models;

import java.util.UUID;

public class Customer {
    private String id;
    private String name;
    private String phone;
    private String contact;
    private double loyaltyPoints;

    public Customer(String phone, String name, String contact) {
        this.id = UUID.randomUUID().toString();
        this.phone = phone;
        this.name = name;
        this.contact = contact;
        this.loyaltyPoints = 0.0;
    }

    public String getId() { return id; }
    public String getPhone() { return phone; }
    public String getName() { return name; }
    public String getContact() { return contact; }
    public double getLoyaltyPoints() { return loyaltyPoints; }

    public void setName(String name) { this.name = name; }
    public void setContact(String contact) { this.contact = contact; }
    public void addPoints(double pts) { this.loyaltyPoints += pts; }
    public void redeemPoints(double pts) { this.loyaltyPoints = Math.max(0, this.loyaltyPoints - pts); }
}
