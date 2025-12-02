package pos.gui;

import pos.models.User;
import pos.services.POSService;

import javax.swing.*;
import java.awt.*;

public class RefundGUI extends JFrame {
    private final POSService posService;
    private final User currentUser;

    private JTextField receiptIdField;
    private JTextArea statusArea;

    public RefundGUI(POSService posService, User currentUser) {
        super("Refunds");
        this.posService = posService;
        this.currentUser = currentUser;

        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout());

        JPanel inputPanel = new JPanel(new GridLayout(2, 2, 8, 8));
        inputPanel.add(new JLabel("Receipt ID:"));
        receiptIdField = new JTextField();
        inputPanel.add(receiptIdField);

        JButton refundButton = new JButton("Process Refund");
        inputPanel.add(new JLabel(""));
        inputPanel.add(refundButton);

        statusArea = new JTextArea();
        statusArea.setEditable(false);

        add(inputPanel, BorderLayout.NORTH);
        add(new JScrollPane(statusArea), BorderLayout.CENTER);

        refundButton.addActionListener(e -> processRefund());

        setSize(450, 300);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    }

    private void processRefund() {
        String receiptId = receiptIdField.getText().trim();
        if (receiptId.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a receipt ID.");
            return;
        }

        boolean ok = posService.refund(receiptId, currentUser);
        if (ok) {
            statusArea.append("Refund processed for receipt: " + receiptId + "\n");
            JOptionPane.showMessageDialog(this, "Refund processed.");
        } else {
            statusArea.append("Refund failed or not found: " + receiptId + "\n");
            JOptionPane.showMessageDialog(this, "Refund failed or not found.");
        }
    }
}
