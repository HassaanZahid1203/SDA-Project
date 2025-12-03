package pos.gui;

import pos.models.User;
import pos.services.POSService;
import pos.models.Transaction;
import pos.models.CartItem;
import pos.models.Product;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

public class RefundGUI extends JFrame {
    private final POSService posService;
    private final User currentUser;

    private JTextField receiptIdField;
    private JTextArea statusArea;
    private JTextArea transactionDetails;
    private JPanel itemsPanel;
    private List<JCheckBox> itemCheckboxes;
    private List<CartItem> currentTransactionItems;
    private Transaction currentTransaction;
    
    public RefundGUI(POSService posService, User currentUser) {
        super("Refunds");
        this.posService = posService;
        this.currentUser = currentUser;
        this.itemCheckboxes = new ArrayList<>();
        this.currentTransactionItems = new ArrayList<>();

        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout());

        // Top panel: Receipt lookup
        JPanel topPanel = new JPanel(new GridLayout(2, 2, 8, 8));
        topPanel.setBorder(BorderFactory.createTitledBorder("Transaction Lookup"));
        topPanel.add(new JLabel("Receipt ID:"));
        receiptIdField = new JTextField();
        topPanel.add(receiptIdField);

        JButton lookupButton = new JButton("Lookup Transaction");
        JButton clearButton = new JButton("Clear Selection");
        topPanel.add(lookupButton);
        topPanel.add(clearButton);

        // Transaction details panel
        JPanel detailsPanel = new JPanel(new BorderLayout());
        transactionDetails = new JTextArea(8, 50);
        transactionDetails.setEditable(false);
        detailsPanel.add(new JScrollPane(transactionDetails), BorderLayout.CENTER);
        detailsPanel.setBorder(BorderFactory.createTitledBorder("Transaction Details"));

        // Items selection panel (initially hidden)
        itemsPanel = new JPanel();
        itemsPanel.setLayout(new BoxLayout(itemsPanel, BoxLayout.Y_AXIS));
        itemsPanel.setBorder(BorderFactory.createTitledBorder("Select Items to Refund"));
        JScrollPane itemsScrollPane = new JScrollPane(itemsPanel);
        itemsScrollPane.setVisible(false);

        // Button panel for refund actions
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        JButton fullRefundButton = new JButton("Full Refund");
        JButton partialRefundButton = new JButton("Partial Refund");
        JButton cancelRefundButton = new JButton("Cancel");
        
        buttonPanel.add(fullRefundButton);
        buttonPanel.add(partialRefundButton);
        buttonPanel.add(cancelRefundButton);

        // Status area
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusArea = new JTextArea(6, 50);
        statusArea.setEditable(false);
        statusPanel.add(new JScrollPane(statusArea), BorderLayout.CENTER);
        statusPanel.setBorder(BorderFactory.createTitledBorder("Refund Status"));

        // Add components to main layout
        add(topPanel, BorderLayout.NORTH);
        add(detailsPanel, BorderLayout.CENTER);
        
        JPanel centerSouthPanel = new JPanel(new BorderLayout());
        centerSouthPanel.add(itemsScrollPane, BorderLayout.CENTER);
        centerSouthPanel.add(buttonPanel, BorderLayout.SOUTH);
        add(centerSouthPanel, BorderLayout.SOUTH);
        
        JPanel eastPanel = new JPanel(new BorderLayout());
        eastPanel.add(statusPanel, BorderLayout.CENTER);
        add(eastPanel, BorderLayout.EAST);

        // Action listeners
        lookupButton.addActionListener(e -> lookupTransaction());
        clearButton.addActionListener(e -> clearSelection());
        fullRefundButton.addActionListener(e -> processFullRefund());
        partialRefundButton.addActionListener(e -> processPartialRefund());
        cancelRefundButton.addActionListener(e -> cancelRefund());

        setSize(900, 700);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    }

    private void lookupTransaction() {
        String receiptId = receiptIdField.getText().trim();
        if (receiptId.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a receipt ID.");
            return;
        }

        currentTransaction = posService.getTransactionById(receiptId);
        if (currentTransaction == null) {
            transactionDetails.setText("Transaction not found: " + receiptId);
            hideItemsPanel();
            return;
        }

        if (currentTransaction.isRefunded()) {
            transactionDetails.setText("Transaction already fully refunded: " + receiptId);
            hideItemsPanel();
            return;
        }

        displayTransactionDetails();
        displayRefundableItems();
    }

    private void displayTransactionDetails() {
        StringBuilder details = new StringBuilder();
        details.append("Transaction ID: ").append(currentTransaction.getId()).append("\n");
        details.append("Date: ").append(currentTransaction.getTimestamp()).append("\n");
        details.append("Cashier: ").append(currentTransaction.getCashier().getUsername())
                .append(" (").append(currentTransaction.getCashier().getRole()).append(")\n");
        details.append("Status: ").append(currentTransaction.isRefunded() ? "FULLY REFUNDED" : "ACTIVE").append("\n");
        details.append("Subtotal: PKR ").append(String.format("%.2f", currentTransaction.getSubtotal())).append("\n");
        details.append("Discount: PKR ").append(String.format("%.2f", currentTransaction.getDiscountTotal())).append("\n");
        details.append("Tax: PKR ").append(String.format("%.2f", currentTransaction.getTaxTotal())).append("\n");
        details.append("Grand Total: PKR ").append(String.format("%.2f", currentTransaction.getGrandTotal())).append("\n\n");
        details.append("Items in Transaction:\n");

        currentTransactionItems = currentTransaction.getItems();
        for (int i = 0; i < currentTransactionItems.size(); i++) {
            CartItem ci = currentTransactionItems.get(i);
            details.append(i + 1).append(". ").append(ci.getProduct().getName())
                   .append(" [").append(ci.getProduct().getBarcode()).append("]")
                   .append(" x").append(ci.getQuantity())
                   .append(" @ PKR ").append(String.format("%.2f", ci.getProduct().getPrice()))
                   .append(" = PKR ").append(String.format("%.2f", ci.getLineSubtotal()))
                   .append("\n");
        }

        transactionDetails.setText(details.toString());
    }

    private void displayRefundableItems() {
        // Clear previous items
        itemsPanel.removeAll();
        itemCheckboxes.clear();
        
        if (currentTransactionItems.isEmpty()) {
            itemsPanel.add(new JLabel("No items in transaction"));
            itemsPanel.getParent().getParent().setVisible(false);
            return;
        }

        // Create checkboxes for each item
        for (int i = 0; i < currentTransactionItems.size(); i++) {
            CartItem ci = currentTransactionItems.get(i);
            JPanel itemPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            
            JCheckBox checkBox = new JCheckBox();
            JLabel itemLabel = new JLabel(
                String.format("%d. %s [%s] x%d = PKR %.2f",
                    i + 1,
                    ci.getProduct().getName(),
                    ci.getProduct().getBarcode(),
                    ci.getQuantity(),
                    ci.getLineSubtotal()
                )
            );
            
            itemCheckboxes.add(checkBox);
            itemPanel.add(checkBox);
            itemPanel.add(itemLabel);
            itemsPanel.add(itemPanel);
        }

        // Add "Select All" checkbox
        JPanel selectAllPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JCheckBox selectAllCheckBox = new JCheckBox("Select All");
        selectAllCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                boolean selectAll = selectAllCheckBox.isSelected();
                for (JCheckBox cb : itemCheckboxes) {
                    cb.setSelected(selectAll);
                }
            }
        });
        selectAllPanel.add(selectAllCheckBox);
        itemsPanel.add(selectAllPanel);

        itemsPanel.revalidate();
        itemsPanel.repaint();
        itemsPanel.getParent().getParent().setVisible(true);
    }

    private void hideItemsPanel() {
        itemsPanel.removeAll();
        itemsPanel.getParent().getParent().setVisible(false);
        itemCheckboxes.clear();
        currentTransactionItems.clear();
    }

    private void clearSelection() {
        receiptIdField.setText("");
        transactionDetails.setText("");
        statusArea.setText("");
        hideItemsPanel();
        currentTransaction = null;
    }

    private void processFullRefund() {
        if (currentTransaction == null) {
            JOptionPane.showMessageDialog(this, "Please lookup a transaction first.");
            return;
        }

        if (currentTransaction.isRefunded()) {
            JOptionPane.showMessageDialog(this, "This transaction is already refunded.");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
            "Are you sure you want to FULLY refund this transaction?\n" +
            "Total Amount: PKR " + String.format("%.2f", currentTransaction.getGrandTotal()) + "\n" +
            "This will restore ALL items stock and mark transaction as refunded.",
            "Confirm Full Refund",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            boolean ok = posService.refund(currentTransaction.getId(), currentUser);
            if (ok) {
                statusArea.append("FULL refund processed successfully for receipt: " + 
                    currentTransaction.getId() + "\n");
                JOptionPane.showMessageDialog(this, "Full refund processed successfully!");
                lookupTransaction(); // Refresh details
            } else {
                statusArea.append("Full refund failed for receipt: " + 
                    currentTransaction.getId() + "\n");
                JOptionPane.showMessageDialog(this, "Refund failed!");
            }
        }
    }

    private void processPartialRefund() {
        if (currentTransaction == null) {
            JOptionPane.showMessageDialog(this, "Please lookup a transaction first.");
            return;
        }

        if (currentTransaction.isRefunded()) {
            JOptionPane.showMessageDialog(this, "This transaction is already fully refunded.");
            return;
        }

        // Check if any items are selected
        List<Integer> selectedIndices = new ArrayList<>();
        for (int i = 0; i < itemCheckboxes.size(); i++) {
            if (itemCheckboxes.get(i).isSelected()) {
                selectedIndices.add(i);
            }
        }

        if (selectedIndices.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select at least one item to refund.");
            return;
        }

        // Calculate refund amount for selected items
        double refundAmount = 0;
        StringBuilder itemsSummary = new StringBuilder("Selected items for refund:\n");
        for (int index : selectedIndices) {
            CartItem ci = currentTransactionItems.get(index);
            refundAmount += ci.getLineSubtotal();
            itemsSummary.append("- ").append(ci.getProduct().getName())
                       .append(" x").append(ci.getQuantity())
                       .append(" = PKR ").append(String.format("%.2f", ci.getLineSubtotal()))
                       .append("\n");
        }

        int confirm = JOptionPane.showConfirmDialog(this,
            itemsSummary.toString() +
            "\nTotal Refund Amount: PKR " + String.format("%.2f", refundAmount) + "\n" +
            "This will restore stock for selected items only.\n" +
            "Transaction will remain active for non-refunded items.",
            "Confirm Partial Refund",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            // Process partial refund
            boolean ok = posService.partialRefund(currentTransaction.getId(), selectedIndices, currentUser);
            if (ok) {
                statusArea.append("PARTIAL refund processed for receipt: " + 
                    currentTransaction.getId() + "\n");
                statusArea.append("Refunded PKR " + String.format("%.2f", refundAmount) + "\n");
                JOptionPane.showMessageDialog(this, "Partial refund processed successfully!");
                lookupTransaction(); // Refresh details
            } else {
                statusArea.append("Partial refund failed for receipt: " + 
                    currentTransaction.getId() + "\n");
                JOptionPane.showMessageDialog(this, "Partial refund failed!");
            }
        }
    }

    private void cancelRefund() {
        clearSelection();
        statusArea.append("Refund operation cancelled.\n");
    }
}
