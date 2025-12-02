package pos.models;

public class CartItem {
    private Product product;
    private int quantity;
    private double lineDiscount;
    private double lineTax;

    public CartItem(Product product, int quantity) {
        this.product = product;
        this.quantity = quantity;
    }

    public Product getProduct() { return product; }
    public int getQuantity() { return quantity; }
    public double getLineDiscount() { return lineDiscount; }
    public double getLineTax() { return lineTax; }

    public void setQuantity(int q) { this.quantity = q; }
    public void setLineDiscount(double d) { this.lineDiscount = d; }
    public void setLineTax(double t) { this.lineTax = t; }

    public double getLineSubtotal() { return product.getPrice() * quantity; }
    public double getLineTotal() { return getLineSubtotal() - lineDiscount + lineTax; }
}
