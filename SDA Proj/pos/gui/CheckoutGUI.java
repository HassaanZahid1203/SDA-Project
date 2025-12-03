package pos.gui;

import pos.models.*;
import pos.services.*;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CheckoutGUI extends JFrame {
    // Services
    private final InventoryManager inventory;
    private final POSService posService;
    private final CustomerService customerService;
    private final ReportService reportService;

    // Transaction + payments
    private Transaction currentTx;
    private final List<Payment> payments = new ArrayList<>();

    // UI components
    private JTextField searchField;
    private JTable cartTable;
    private DefaultTableModel cartModel;
    private JLabel subtotalLabel, discountLabel, taxLabel, totalLabel, remainingLabel;
    private JButton removeSelectedBtn; // New button

    public CheckoutGUI(User user) throws IOException {
        super("POS System - Checkout (" + user.getRole() + ")");

        // Init services
        inventory = new InventoryManager("stock.txt");
        inventory.ensureInventoryFile();
        inventory.load();

        ReceiptWriter receiptWriter = new ReceiptWriter("receipts");
        PromotionService promoService = new PromotionService();
        customerService = new CustomerService();
        
        // Create TransactionStorage
        TransactionStorage transactionStorage = new TransactionStorage("transactions", inventory);

        posService = new POSService(inventory, receiptWriter, promoService, customerService, transactionStorage);
        reportService = new ReportService(posService);
        posService.setReportService(reportService);

        // Start transaction
        currentTx = posService.startTransaction(user);

        initUI(user);
        recalcTotals();
    }

    private void initUI(User user) {
        setLayout(new BorderLayout());

        // Top: search + add item
        JPanel topPanel = new JPanel(new BorderLayout(8, 8));
        searchField = new JTextField();
        JButton addButton = new JButton("Add item");
        JButton promoButton = new JButton("Apply promo");
        JPanel rightTop = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        rightTop.add(promoButton);
        rightTop.add(addButton);

        topPanel.add(new JLabel("Scan barcode or type name:"), BorderLayout.WEST);
        topPanel.add(searchField, BorderLayout.CENTER);
        topPanel.add(rightTop, BorderLayout.EAST);

        // Center: cart table
        cartModel = new DefaultTableModel(new Object[]{"Barcode", "Name", "Qty", "Price", "Subtotal"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                // Allow editing quantity directly in table
                return col == 2;
            }
        };
        cartTable = new JTable(cartModel);
        cartTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        
        // Set column widths
        cartTable.getColumnModel().getColumn(0).setPreferredWidth(80);  // Barcode
        cartTable.getColumnModel().getColumn(1).setPreferredWidth(150); // Name
        cartTable.getColumnModel().getColumn(2).setPreferredWidth(50);  // Qty
        cartTable.getColumnModel().getColumn(3).setPreferredWidth(70);  // Price
        cartTable.getColumnModel().getColumn(4).setPreferredWidth(80);  // Subtotal

        // East: totals
        JPanel totalsPanel = new JPanel(new GridLayout(5, 1, 6, 6));
        subtotalLabel = new JLabel("Subtotal: PKR 0.00");
        discountLabel = new JLabel("Discounts: PKR 0.00");
        taxLabel = new JLabel("Tax: PKR 0.00");
        totalLabel = new JLabel("Grand total: PKR 0.00");
        remainingLabel = new JLabel("Remaining: PKR 0.00");
        totalsPanel.setBorder(BorderFactory.createTitledBorder("Totals"));
        totalsPanel.add(subtotalLabel);
        totalsPanel.add(discountLabel);
        totalsPanel.add(taxLabel);
        totalsPanel.add(totalLabel);
        totalsPanel.add(remainingLabel);

        // South: payment panel (buttons)
        JPanel paymentPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        JButton cashBtn = new JButton("Cash");
        JButton cardBtn = new JButton("Card");
        JButton loyaltyBtn = new JButton("Loyalty");
        JButton completeBtn = new JButton("Complete sale");
        JButton clearPaymentsBtn = new JButton("Clear payments");
        JButton clearCartBtn = new JButton("Clear Cart");
        removeSelectedBtn = new JButton("Remove Selected"); // New button
        removeSelectedBtn.setForeground(Color.RED);
        JButton customerBtn = new JButton("Attach Customer");
        customerBtn.setForeground(Color.BLUE);
        paymentPanel.add(customerBtn);
       
        JButton logoutButton = new JButton("Logout");
        logoutButton.setForeground(Color.RED);
        logoutButton.setFont(new Font("Arial", Font.BOLD, 12));

        logoutButton.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to logout?\nAny unsaved transaction will be lost.",
                "Confirm Logout",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
            );

            if (confirm == JOptionPane.YES_OPTION) {
                // Close current checkout window
                dispose();
                // Return to login screen
                SwingUtilities.invokeLater(() -> {
                    try {
                        new LoginGUI().setVisible(true);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                });
            }
        });

        paymentPanel.setBorder(BorderFactory.createTitledBorder("Payments"));
        paymentPanel.add(cashBtn);
        paymentPanel.add(cardBtn);
        paymentPanel.add(loyaltyBtn);
        paymentPanel.add(completeBtn);
        paymentPanel.add(clearPaymentsBtn);
        paymentPanel.add(removeSelectedBtn); // Add remove button
        paymentPanel.add(clearCartBtn);
        paymentPanel.add(logoutButton);

        // West: navigation (role-based)
        JPanel navPanel = new JPanel(new GridLayout(0, 1, 8, 8));
        navPanel.setBorder(BorderFactory.createTitledBorder("Navigation"));
        JButton customersBtn = new JButton("Customers");
        JButton refundsBtn = new JButton("Refunds");
        navPanel.add(customersBtn);
        navPanel.add(refundsBtn);

        if (roleIs(user, "Manager") || roleIs(user, "Admin")) {
            JButton reportsBtn = new JButton("Reports");
            navPanel.add(reportsBtn);
            reportsBtn.addActionListener(e -> new ReportsGUI(reportService).setVisible(true));
        }
        if (roleIs(user, "Admin")) {
            JButton inventoryBtn = new JButton("Inventory");
            navPanel.add(inventoryBtn);
            inventoryBtn.addActionListener(e -> new InventoryGUI(inventory).setVisible(true));
        }

        // Add components
        add(topPanel, BorderLayout.NORTH);
        add(new JScrollPane(cartTable), BorderLayout.CENTER);
        add(totalsPanel, BorderLayout.EAST);
        add(paymentPanel, BorderLayout.SOUTH);
        add(navPanel, BorderLayout.WEST);

        // Actions
        addButton.addActionListener(e -> addItem());
        promoButton.addActionListener(e -> applyPromo());
        customersBtn.addActionListener(e -> new CustomerGUI(customerService).setVisible(true));
        refundsBtn.addActionListener(e -> new RefundGUI(posService, currentTx.getCashier()).setVisible(true));
        clearCartBtn.addActionListener(e -> clearCart());
        removeSelectedBtn.addActionListener(e -> removeSelectedItem()); // Add action for remove button

        // Update qty inline in cart table
        cartModel.addTableModelListener(e -> {
            if (e.getColumn() == 2) {
                // When qty changes in table, re-run items in transaction accordingly
                try {
                    refreshTransactionFromTable();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Invalid quantity change.");
                }
            }
        });

        // Payment actions
        cashBtn.addActionListener(e -> payCash());
        cardBtn.addActionListener(e -> payCard());
        loyaltyBtn.addActionListener(e -> payLoyalty());
        completeBtn.addActionListener(e -> completeSale());
        clearPaymentsBtn.addActionListener(e -> clearPayments());
        customerBtn.addActionListener(e -> attachCustomer());

        setSize(1200, 650);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    private boolean roleIs(User u, String role) {
        return u.getRole() != null && u.getRole().equalsIgnoreCase(role);
    }

    private void addItem() {
        String query = searchField.getText().trim();
        if (query.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Enter barcode or product name.");
            return;
        }
        Product p = inventory.findByQuery(query);
        if (p == null) {
            JOptionPane.showMessageDialog(this, "Item not found.");
            return;
        }
        String qtyStr = JOptionPane.showInputDialog(this, "Enter quantity for " + p.getName(), "1");
        try {
            int qty = Integer.parseInt(qtyStr);
            if (qty <= 0) throw new NumberFormatException();
            if (!posService.addItem(currentTx, p.getBarcode(), qty)) {
                JOptionPane.showMessageDialog(this, "Insufficient stock.");
                return;
            }
            // Add row or merge if already in cart
            boolean merged = false;
            for (int i = 0; i < cartModel.getRowCount(); i++) {
                String b = cartModel.getValueAt(i, 0).toString();
                if (b.equals(p.getBarcode())) {
                    int existingQty = Integer.parseInt(cartModel.getValueAt(i, 2).toString());
                    int newQty = existingQty + qty;
                    cartModel.setValueAt(newQty, i, 2);
                    cartModel.setValueAt(String.format("%.2f", p.getPrice() * newQty), i, 4);
                    merged = true;
                    break;
                }
            }
            if (!merged) {
                cartModel.addRow(new Object[]{
                    p.getBarcode(), 
                    p.getName(), 
                    qty, 
                    String.format("%.2f", p.getPrice()), 
                    String.format("%.2f", p.getPrice() * qty)
                });
            }
            recalcTotals();
            searchField.setText(""); // Clear search field after adding
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Invalid quantity.");
        }
    }

    private void attachCustomer() {
        String phone = JOptionPane.showInputDialog(this, "Enter customer phone number:");
        if (phone != null && !phone.trim().isEmpty()) {
            Customer c = customerService.attachOrCreate(phone.trim());
            currentTx.setCustomer(c);
            JOptionPane.showMessageDialog(this, 
                "Customer attached: " + c.getName() + " (" + phone + ")\n" +
                "Loyalty Points: " + String.format("%.2f", c.getLoyaltyPoints()));
        }
    }

    private void refreshTransactionFromTable() {
        // Reset the transaction items and rebuild from table rows
        User cashier = currentTx.getCashier();
        currentTx = posService.startTransaction(cashier);
        for (int i = 0; i < cartModel.getRowCount(); i++) {
            String barcode = cartModel.getValueAt(i, 0).toString();
            int qty = Integer.parseInt(cartModel.getValueAt(i, 2).toString());
            if (!posService.addItem(currentTx, barcode, qty)) {
                JOptionPane.showMessageDialog(this, "Insufficient stock for " + barcode);
                return;
            }
            // Update subtotal cell
            Product p = inventory.findByBarcode(barcode);
            if (p != null) {
                cartModel.setValueAt(String.format("%.2f", p.getPrice() * qty), i, 4);
            }
        }
        recalcTotals();
    }

    private void applyPromo() {
        String code = JOptionPane.showInputDialog(this, "Enter promo code:");
        if (code != null && !code.trim().isEmpty()) {
            posService.applyPromoCode(currentTx, code.trim());
            recalcTotals();
        }
    }

    private void recalcTotals() {
        posService.recalculateTotals(currentTx);
        subtotalLabel.setText("Subtotal: PKR " + String.format("%.2f", currentTx.getSubtotal()));
        discountLabel.setText("Discounts: PKR " + String.format("%.2f", currentTx.getDiscountTotal()));
        taxLabel.setText("Tax: PKR " + String.format("%.2f", currentTx.getTaxTotal()));
        totalLabel.setText("Grand total: PKR " + String.format("%.2f", currentTx.getGrandTotal()));

        double paid = payments.stream().mapToDouble(Payment::getAmount).sum();
        double remaining = currentTx.getGrandTotal() - paid;
        remainingLabel.setText("Remaining: PKR " + String.format("%.2f", Math.max(remaining, 0)));
    }

    private void removeSelectedItem() {
        int selectedRow = cartTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select an item to remove.");
            return;
        }
        
        String barcode = cartModel.getValueAt(selectedRow, 0).toString();
        int qty = Integer.parseInt(cartModel.getValueAt(selectedRow, 2).toString());
        
        // Ask for confirmation
        int confirm = JOptionPane.showConfirmDialog(
            this,
            "Remove " + cartModel.getValueAt(selectedRow, 1) + " (x" + qty + ") from cart?",
            "Confirm Removal",
            JOptionPane.YES_NO_OPTION
        );
        
        if (confirm == JOptionPane.YES_OPTION) {
            // Release stock back to inventory
            inventory.releaseStock(barcode, qty);
            
            // Remove row from table
            cartModel.removeRow(selectedRow);
            
            // Refresh transaction from remaining items
            refreshTransactionFromTable();
            
            JOptionPane.showMessageDialog(this, "Item removed from cart.");
        }
    }

    private void clearCart() {
        if (cartModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "Cart is already empty.");
            return;
        }
        
        int confirm = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to clear the entire cart?\nAll items will be removed.",
            "Clear Cart",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );
        
        if (confirm == JOptionPane.YES_OPTION) {
            // Release stock for all items
            for (int i = 0; i < cartModel.getRowCount(); i++) {
                String barcode = cartModel.getValueAt(i, 0).toString();
                int qty = Integer.parseInt(cartModel.getValueAt(i, 2).toString());
                inventory.releaseStock(barcode, qty);
            }
            
            // Clear cart table
            cartModel.setRowCount(0);
            
            // Create new transaction
            currentTx = posService.startTransaction(currentTx.getCashier());
            recalcTotals();
            
            JOptionPane.showMessageDialog(this, "Cart cleared.");
        }
    }

    private void payCash() {
        double remaining = currentTx.getGrandTotal() - payments.stream().mapToDouble(Payment::getAmount).sum();
        if (remaining <= 0.01) {
            JOptionPane.showMessageDialog(this, "Already fully paid.");
            return;
        }
        String amtStr = JOptionPane.showInputDialog(this, "Cash received (Remaining: " + String.format("%.2f", remaining) + "):", String.format("%.2f", remaining));
        if (amtStr == null) return;
        try {
            double amt = Double.parseDouble(amtStr);
            if (amt <= 0) throw new NumberFormatException();
            payments.add(new Payment("Cash", amt));
            double change = amt - remaining;
            if (change > 0.009) {
                JOptionPane.showMessageDialog(this, "Change to return: PKR " + String.format("%.2f", change));
            }
            recalcTotals();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Invalid amount.");
        }
    }

    private void payCard() {
        double remaining = currentTx.getGrandTotal() - payments.stream().mapToDouble(Payment::getAmount).sum();
        if (remaining <= 0.01) {
            JOptionPane.showMessageDialog(this, "Already fully paid.");
            return;
        }
        String amtStr = JOptionPane.showInputDialog(this, "Card amount (Remaining: " + String.format("%.2f", remaining) + "):", String.format("%.2f", remaining));
        if (amtStr == null) return;
        try {
            double amt = Double.parseDouble(amtStr);
            if (amt <= 0) throw new NumberFormatException();
            payments.add(new Payment("Card", amt));
            recalcTotals();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Invalid amount.");
        }
    }

    private void payLoyalty() {
        double remaining = currentTx.getGrandTotal() - payments.stream().mapToDouble(Payment::getAmount).sum();
        if (remaining <= 0.01) {
            JOptionPane.showMessageDialog(this, "Already fully paid.");
            return;
        }
        String amtStr = JOptionPane.showInputDialog(this, "Loyalty amount (Remaining: " + String.format("%.2f", remaining) + "):", "0.00");
        if (amtStr == null) return;
        try {
            double amt = Double.parseDouble(amtStr);
            if (amt < 0) throw new NumberFormatException();
            payments.add(new Payment("Loyalty", amt));
            recalcTotals();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Invalid amount.");
        }
    }

    private void clearPayments() {
        payments.clear();
        recalcTotals();
        JOptionPane.showMessageDialog(this, "Payments cleared.");
    }

    private void completeSale() {
        double total = currentTx.getGrandTotal();
        double paid = payments.stream().mapToDouble(Payment::getAmount).sum();
        double remaining = total - paid;

        if (remaining > 0.01) {
            JOptionPane.showMessageDialog(this, "Remaining amount not fully paid: PKR " + String.format("%.2f", remaining));
            return;
        }

        if (posService.completeSale(currentTx, payments)) {
            JOptionPane.showMessageDialog(this, "Sale complete!\nReceipt: " + currentTx.getReceiptPath());
            cartModel.setRowCount(0);
            payments.clear();
            currentTx = posService.startTransaction(currentTx.getCashier());
            recalcTotals();
        } else {
            JOptionPane.showMessageDialog(this, "Sale failed.");
        }
        
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                // Demo user; in production launch from LoginGUI
                User user = new User("cashier1", "Cashier");
                new CheckoutGUI(user).setVisible(true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}