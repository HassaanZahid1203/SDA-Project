package pos.gui;

import pos.models.Customer;
import pos.services.CustomerService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class CustomerGUI extends JFrame {
    private final CustomerService customerService;

    private JTextField phoneField, nameField, contactField;
    private JTextField searchField;
    private JTable resultsTable;
    private DefaultTableModel resultsModel;

    public CustomerGUI(CustomerService customerService) {
        super("Customer Management");
        this.customerService = customerService;
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout());

        JPanel createPanel = new JPanel(new GridLayout(5, 2, 8, 8));
        createPanel.setBorder(BorderFactory.createTitledBorder("Create / Update Customer"));

        phoneField = new JTextField();
        nameField = new JTextField();
        contactField = new JTextField();
        JButton saveButton = new JButton("Save");
        JButton deleteButton = new JButton("Delete");
        deleteButton.setForeground(Color.RED);

        createPanel.add(new JLabel("Phone*:")); createPanel.add(phoneField);
        createPanel.add(new JLabel("Name*:")); createPanel.add(nameField);
        createPanel.add(new JLabel("Contact:")); createPanel.add(contactField);
        createPanel.add(new JLabel("")); createPanel.add(saveButton);
        createPanel.add(new JLabel("")); createPanel.add(deleteButton);

        saveButton.addActionListener(e -> saveCustomer());
        deleteButton.addActionListener(e -> deleteCustomer());

        JPanel searchPanel = new JPanel(new BorderLayout(8, 8));
        searchPanel.setBorder(BorderFactory.createTitledBorder("Search Customers"));
        searchField = new JTextField();
        JButton searchButton = new JButton("Search");
        JButton refreshButton = new JButton("Refresh All");
        JPanel searchButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        searchButtons.add(searchButton);
        searchButtons.add(refreshButton);
        searchPanel.add(searchField, BorderLayout.CENTER);
        searchPanel.add(searchButtons, BorderLayout.EAST);

        searchButton.addActionListener(e -> searchCustomers());
        refreshButton.addActionListener(e -> refreshAllCustomers());

        resultsModel = new DefaultTableModel(new Object[]{"Phone", "Name", "Contact", "Loyalty Points", "Customer ID"}, 0);
        resultsTable = new JTable(resultsModel);
        resultsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        JScrollPane tableScroll = new JScrollPane(resultsTable);
        
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBorder(BorderFactory.createTitledBorder("Customer List"));
        tablePanel.add(tableScroll, BorderLayout.CENTER);
        
        JPanel loyaltyPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        JButton addPointsBtn = new JButton("Add Points");
        JButton redeemPointsBtn = new JButton("Redeem Points");
        loyaltyPanel.add(addPointsBtn);
        loyaltyPanel.add(redeemPointsBtn);
        tablePanel.add(loyaltyPanel, BorderLayout.SOUTH);
        
        addPointsBtn.addActionListener(e -> modifyLoyaltyPoints(true));
        redeemPointsBtn.addActionListener(e -> modifyLoyaltyPoints(false));

        add(createPanel, BorderLayout.NORTH);
        add(searchPanel, BorderLayout.CENTER);
        add(tablePanel, BorderLayout.SOUTH);

        resultsTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedRow = resultsTable.getSelectedRow();
                if (selectedRow >= 0) {
                    phoneField.setText(resultsModel.getValueAt(selectedRow, 0).toString());
                    nameField.setText(resultsModel.getValueAt(selectedRow, 1).toString());
                    contactField.setText(resultsModel.getValueAt(selectedRow, 2).toString());
                }
            }
        });

        setSize(800, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        
        refreshAllCustomers();
    }

    private void saveCustomer() {
        String phone = phoneField.getText().trim();
        String name = nameField.getText().trim();
        String contact = contactField.getText().trim();

        if (phone.isEmpty() || name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Phone and Name are required.");
            return;
        }

        Customer c = customerService.createOrUpdate(phone, name, contact);
        JOptionPane.showMessageDialog(this, 
            "Saved: " + c.getName() + " (" + c.getPhone() + ")\n" +
            "Loyalty Points: " + String.format("%.2f", c.getLoyaltyPoints()));
        
        phoneField.setText(""); 
        nameField.setText(""); 
        contactField.setText("");
        
        refreshAllCustomers();
    }

    private void deleteCustomer() {
        String phone = phoneField.getText().trim();
        if (phone.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Enter phone number to delete.");
            return;
        }
        
        int confirm = JOptionPane.showConfirmDialog(this,
            "Delete customer: " + phone + "?\nThis action cannot be undone.",
            "Confirm Delete",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);
            
        if (confirm == JOptionPane.YES_OPTION) {
            customerService.deleteCustomer(phone);
            JOptionPane.showMessageDialog(this, "Customer deleted: " + phone);
            
            phoneField.setText(""); 
            nameField.setText(""); 
            contactField.setText("");
            
            refreshAllCustomers();
        }
    }

    private void searchCustomers() {
        String q = searchField.getText().trim();
        resultsModel.setRowCount(0);
        if (q.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Enter a name, phone, or ID to search.");
            return;
        }
        List<Customer> list = customerService.search(q);
        if (list.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No results found.");
        } else {
            for (Customer cu : list) {
                resultsModel.addRow(new Object[]{
                    cu.getPhone(), 
                    cu.getName(), 
                    cu.getContact(), 
                    String.format("%.2f", cu.getLoyaltyPoints()),
                    cu.getId()
                });
            }
        }
    }

    private void refreshAllCustomers() {
        resultsModel.setRowCount(0);
        List<Customer> allCustomers = customerService.getAllCustomers();
        if (allCustomers.isEmpty()) {
            resultsModel.addRow(new Object[]{"No customers found", "", "", "", ""});
        } else {
            for (Customer cu : allCustomers) {
                resultsModel.addRow(new Object[]{
                    cu.getPhone(), 
                    cu.getName(), 
                    cu.getContact(), 
                    String.format("%.2f", cu.getLoyaltyPoints()),
                    cu.getId()
                });
            }
        }
    }
    
    private void modifyLoyaltyPoints(boolean isAdd) {
        int selectedRow = resultsTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select a customer first.");
            return;
        }
        
        String phone = resultsModel.getValueAt(selectedRow, 0).toString();
        String customerName = resultsModel.getValueAt(selectedRow, 1).toString();
        double currentPoints = Double.parseDouble(resultsModel.getValueAt(selectedRow, 3).toString());
        
        String action = isAdd ? "Add" : "Redeem";
        String message = action + " loyalty points for " + customerName + "\n" +
                        "Current Points: " + String.format("%.2f", currentPoints) + "\n" +
                        "Enter points amount:";
        
        String pointsStr = JOptionPane.showInputDialog(this, message, "0.00");
        if (pointsStr != null && !pointsStr.trim().isEmpty()) {
            try {
                double points = Double.parseDouble(pointsStr);
                if (points <= 0) {
                    JOptionPane.showMessageDialog(this, "Points must be greater than 0.");
                    return;
                }
                
                if (isAdd) {
                    customerService.addLoyaltyPoints(phone, points);
                    currentPoints += points;
                    JOptionPane.showMessageDialog(this, 
                        "Added " + String.format("%.2f", points) + " points to " + customerName + "\n" +
                        "New Total: " + String.format("%.2f", currentPoints));
                } else {
                    if (points > currentPoints) {
                        JOptionPane.showMessageDialog(this, 
                            "Cannot redeem " + String.format("%.2f", points) + " points.\n" +
                            "Available: " + String.format("%.2f", currentPoints));
                        return;
                    }
                    customerService.redeemLoyaltyPoints(phone, points);
                    currentPoints -= points;
                    JOptionPane.showMessageDialog(this, 
                        "Redeemed " + String.format("%.2f", points) + " points from " + customerName + "\n" +
                        "Remaining: " + String.format("%.2f", currentPoints));
                }
                
                resultsModel.setValueAt(String.format("%.2f", currentPoints), selectedRow, 3);
                
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Invalid points amount.");
            }
        }
    }
}