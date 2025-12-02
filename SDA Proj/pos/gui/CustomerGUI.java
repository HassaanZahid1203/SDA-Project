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

        // Create/Update panel
        JPanel createPanel = new JPanel(new GridLayout(4, 2, 8, 8));
        createPanel.setBorder(BorderFactory.createTitledBorder("Create / Update Customer"));

        phoneField = new JTextField();
        nameField = new JTextField();
        contactField = new JTextField();
        JButton saveButton = new JButton("Save");

        createPanel.add(new JLabel("Phone:")); createPanel.add(phoneField);
        createPanel.add(new JLabel("Name:")); createPanel.add(nameField);
        createPanel.add(new JLabel("Contact:")); createPanel.add(contactField);
        createPanel.add(new JLabel("")); createPanel.add(saveButton);

        saveButton.addActionListener(e -> saveCustomer());

        // Search panel
        JPanel searchPanel = new JPanel(new BorderLayout(8, 8));
        searchPanel.setBorder(BorderFactory.createTitledBorder("Search Customers"));
        searchField = new JTextField();
        JButton searchButton = new JButton("Search");
        searchPanel.add(searchField, BorderLayout.CENTER);
        searchPanel.add(searchButton, BorderLayout.EAST);

        searchButton.addActionListener(e -> searchCustomers());

        // Results table
        resultsModel = new DefaultTableModel(new Object[]{"ID", "Name", "Phone", "Contact", "Points"}, 0);
        resultsTable = new JTable(resultsModel);

        add(createPanel, BorderLayout.NORTH);
        add(searchPanel, BorderLayout.CENTER);
        add(new JScrollPane(resultsTable), BorderLayout.SOUTH);

        setSize(700, 500);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
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
        JOptionPane.showMessageDialog(this, "Saved: " + c.getName() + " (" + c.getPhone() + ")");
        phoneField.setText(""); nameField.setText(""); contactField.setText("");
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
                        cu.getId(), cu.getName(), cu.getPhone(), cu.getContact(), String.format("%.2f", cu.getLoyaltyPoints())
                });
            }
        }
    }
}
