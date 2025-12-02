package pos.gui;

import pos.models.Product;
import pos.services.InventoryManager;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.IOException;
import java.util.Collection;
import java.util.Locale;

public class InventoryGUI extends JFrame {
    private final InventoryManager inventory;
    private JTable table;
    private DefaultTableModel model;

    public InventoryGUI(InventoryManager inventory) {
        super("Inventory Management");
        this.inventory = inventory;
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout());

        model = new DefaultTableModel(new Object[]{
                "Barcode", "Name", "Price", "Stock", "LowStockThreshold", "TaxRate"
        }, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                // Allow editing of price, stock, threshold, tax
                return col == 2 || col == 3 || col == 4 || col == 5;
            }
        };

        table = new JTable(model);
        loadTable();

        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        JButton refreshButton = new JButton("Refresh");
        JButton lowStockButton = new JButton("Show Low Stock");
        JButton saveButton = new JButton("Save Changes");
        JButton restockButton = new JButton("Restock Selected");

        buttonsPanel.add(refreshButton);
        buttonsPanel.add(lowStockButton);
        buttonsPanel.add(saveButton);
        buttonsPanel.add(restockButton);

        refreshButton.addActionListener(e -> loadTable());
        lowStockButton.addActionListener(e -> showLowStock());
        saveButton.addActionListener(e -> saveChanges());
        restockButton.addActionListener(e -> restockSelected());

        add(new JScrollPane(table), BorderLayout.CENTER);
        add(buttonsPanel, BorderLayout.NORTH);

        setSize(900, 500);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    }

    private void loadTable() {
        model.setRowCount(0);
        try {
            inventory.load();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Failed to reload inventory: " + e.getMessage());
        }

        Collection<Product> products = inventory.getAllProducts(); // add this method in InventoryManager
        for (Product p : products) {
            model.addRow(new Object[]{
                    p.getBarcode(),
                    p.getName(),
                    String.format(Locale.US, "%.2f", p.getPrice()),
                    p.getStock(),
                    p.getLowStockThreshold(),
                    String.format(Locale.US, "%.2f", p.getTaxRate())
            });
        }
    }

    private void showLowStock() {
        StringBuilder sb = new StringBuilder("Low stock items:\n");
        for (int i = 0; i < model.getRowCount(); i++) {
            int stock = Integer.parseInt(model.getValueAt(i, 3).toString());
            int threshold = Integer.parseInt(model.getValueAt(i, 4).toString());
            if (stock <= threshold) {
                sb.append(model.getValueAt(i, 0)).append(" | ")
                  .append(model.getValueAt(i, 1)).append(" | Stock: ")
                  .append(stock).append("\n");
            }
        }
        JOptionPane.showMessageDialog(this, sb.length() > 18 ? sb.toString() : "No low stock items.");
    }

    private void saveChanges() {
        for (int i = 0; i < model.getRowCount(); i++) {
            String barcode = model.getValueAt(i, 0).toString();
            Product p = inventory.findByBarcode(barcode);
            if (p != null) {
                try {
                    double price = Double.parseDouble(model.getValueAt(i, 2).toString());
                    int stock = Integer.parseInt(model.getValueAt(i, 3).toString());
                    int lst = Integer.parseInt(model.getValueAt(i, 4).toString());
                    double tax = Double.parseDouble(model.getValueAt(i, 5).toString());

                    p.setPrice(price);
                    p.setStock(stock);
                    p.setLowStockThreshold(lst);
                    p.setTaxRate(tax);
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "Invalid numeric value in row " + (i + 1));
                }
            }
        }
        try {
            inventory.save();
            JOptionPane.showMessageDialog(this, "Inventory saved to file.");
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Failed to save: " + e.getMessage());
        }
    }

    private void restockSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a product row.");
            return;
        }
        String barcode = model.getValueAt(row, 0).toString();
        Product p = inventory.findByBarcode(barcode);
        if (p == null) {
            JOptionPane.showMessageDialog(this, "Product not found.");
            return;
        }
        String qtyStr = JOptionPane.showInputDialog(this, "Add quantity:");
        try {
            int qty = Integer.parseInt(qtyStr);
            if (qty <= 0) {
                JOptionPane.showMessageDialog(this, "Quantity must be > 0.");
                return;
            }
            p.setStock(p.getStock() + qty);
            model.setValueAt(p.getStock(), row, 3);
            inventory.save();
            JOptionPane.showMessageDialog(this, "Restocked and saved.");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Invalid quantity.");
        }
    }
}
