package pos.models;

public class Product {
    private String barcode;
    private String name;
    private double price;
    private int stock;
    private int lowStockThreshold;
    private double taxRate;

    public Product(String barcode, String name, double price, int stock, int lowStockThreshold, double taxRate) {
        this.barcode = barcode;
        this.name = name;
        this.price = price;
        this.stock = stock;
        this.lowStockThreshold = lowStockThreshold;
        this.taxRate = taxRate;
    }

    public String getBarcode() { return barcode; }
    public String getName() { return name; }
    public double getPrice() { return price; }
    public int getStock() { return stock; }
    public int getLowStockThreshold() { return lowStockThreshold; }
    public double getTaxRate() { return taxRate; }
    public void setPrice(double price) { this.price = price; }
    public void setLowStockThreshold(int threshold) { this.lowStockThreshold = threshold; }
    public void setTaxRate(double taxRate) { this.taxRate = taxRate; }

    public void setStock(int stock) { this.stock = stock; }
}
