package pos.gui;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class RefundLogViewerGUI extends JFrame {
    private JTextArea logArea;
    
    public RefundLogViewerGUI() {
        super("Refund Log Viewer");
        initUI();
        loadLogFile();
    }
    
    private void initUI() {
        setLayout(new BorderLayout());
        
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        
        JScrollPane scrollPane = new JScrollPane(logArea);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton refreshButton = new JButton("Refresh");
        JButton clearButton = new JButton("Clear Log");
        JButton closeButton = new JButton("Close");
        
        buttonPanel.add(refreshButton);
        buttonPanel.add(clearButton);
        buttonPanel.add(closeButton);
        
        add(scrollPane, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
        
        refreshButton.addActionListener(e -> loadLogFile());
        clearButton.addActionListener(e -> clearLogFile());
        closeButton.addActionListener(e -> dispose());
        
        setSize(800, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    }
    
    private void loadLogFile() {
        try {
            String content = new String(Files.readAllBytes(Paths.get("refunds_log.txt")));
            logArea.setText(content);
            logArea.setCaretPosition(0); // Scroll to top
        } catch (IOException e) {
            logArea.setText("Error loading refund log: " + e.getMessage());
        }
    }
    
    private void clearLogFile() {
        int confirm = JOptionPane.showConfirmDialog(this,
            "Are you sure you want to clear the refund log?\nThis action cannot be undone.",
            "Confirm Clear Log",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);
            
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                Files.write(Paths.get("refunds_log.txt"), 
                    ("Log cleared: " + java.time.LocalDateTime.now() + "\n").getBytes());
                loadLogFile();
                JOptionPane.showMessageDialog(this, "Refund log cleared.");
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Failed to clear log: " + e.getMessage());
            }
        }
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new RefundLogViewerGUI().setVisible(true));
    }
}