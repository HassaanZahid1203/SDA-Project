package pos.services;

import pos.models.*;
import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class RefundLogger {
    private final String logFilePath;
    
    public RefundLogger(String logFilePath) {
        this.logFilePath = logFilePath;
        ensureLogFile();
    }
    
    private void ensureLogFile() {
        try {
            Path path = Paths.get(logFilePath);
            if (!Files.exists(path)) {
                // Create log file with headers
                StringBuilder header = new StringBuilder();
                header.append("================================================================================\n");
                header.append("REFUND LOG FILE\n");
                header.append("Created: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
                header.append("================================================================================\n");
                header.append("TIMESTAMP | REFUND TYPE | RECEIPT ID | PROCESSED BY | AMOUNT | ITEMS\n");
                header.append("--------------------------------------------------------------------------------\n");
                
                Files.write(path, header.toString().getBytes());
                System.out.println("Created refund log file: " + path.toAbsolutePath());
            }
        } catch (IOException e) {
            System.err.println("Failed to create refund log file: " + e.getMessage());
        }
    }
    
    public void logFullRefund(Transaction tx, User processedBy) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String logEntry = String.format("%s | FULL      | %s | %s (%s) | PKR %.2f | ALL items (%d items)\n",
            timestamp,
            tx.getId(),
            processedBy.getUsername(),
            processedBy.getRole(),
            tx.getGrandTotal(),
            tx.getItems().size()
        );
        
        writeToLog(logEntry);
    }
    
    public void logPartialRefund(Transaction tx, List<CartItem> refundedItems, double refundAmount, User processedBy) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        
        StringBuilder itemsDesc = new StringBuilder();
        for (CartItem ci : refundedItems) {
            itemsDesc.append(ci.getProduct().getName())
                    .append("(x").append(ci.getQuantity()).append("), ");
        }
        if (itemsDesc.length() > 2) {
            itemsDesc.setLength(itemsDesc.length() - 2); // Remove trailing comma
        }
        
        String logEntry = String.format("%s | PARTIAL   | %s | %s (%s) | PKR %.2f | %s\n",
            timestamp,
            tx.getId(),
            processedBy.getUsername(),
            processedBy.getRole(),
            refundAmount,
            itemsDesc.toString()
        );
        
        writeToLog(logEntry);
    }
    
    private void writeToLog(String logEntry) {
        try {
            Files.write(Paths.get(logFilePath), logEntry.getBytes(), StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println("Failed to write to refund log: " + e.getMessage());
        }
    }
    
    public String getLogContent() {
        try {
            return new String(Files.readAllBytes(Paths.get(logFilePath)));
        } catch (IOException e) {
            return "Unable to read refund log: " + e.getMessage();
        }
    }
    
    public void clearLog() {
        try {
            ensureLogFile(); // This will recreate with headers
        } catch (Exception e) {
            System.err.println("Failed to clear log: " + e.getMessage());
        }
    }
}
