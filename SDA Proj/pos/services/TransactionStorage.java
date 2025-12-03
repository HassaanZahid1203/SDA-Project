package pos.services;

import pos.models.Transaction;
import pos.models.CartItem;
import pos.models.Product;
import pos.models.User;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class TransactionStorage {
    private final String transactionsDir;
    private final InventoryManager inventoryManager;
    
    public TransactionStorage(String transactionsDir, InventoryManager inventoryManager) {
        this.transactionsDir = transactionsDir;
        this.inventoryManager = inventoryManager;
        ensureTransactionsDirectory();
    }
    
    private void ensureTransactionsDirectory() {
        try {
            Path dir = Paths.get(transactionsDir);
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
                System.out.println("Created transactions directory: " + dir.toAbsolutePath());
            }
        } catch (IOException e) {
            System.err.println("Failed to create transactions directory: " + e.getMessage());
        }
    }
    
    public void saveTransaction(Transaction tx) throws IOException {
        // Save each transaction as separate file for simplicity
        String fileName = "tx_" + tx.getId() + ".dat";
        Path filePath = Paths.get(transactionsDir, fileName);
        
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filePath.toFile()))) {
            // Create a simplified serializable version
            Map<String, Object> data = new HashMap<>();
            data.put("id", tx.getId());
            data.put("timestamp", tx.getTimestamp().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            data.put("cashierUsername", tx.getCashier().getUsername());
            data.put("cashierRole", tx.getCashier().getRole());
            
            // Store items as list of maps
            List<Map<String, Object>> itemsData = new ArrayList<>();
            for (CartItem ci : tx.getItems()) {
                Map<String, Object> itemData = new HashMap<>();
                itemData.put("barcode", ci.getProduct().getBarcode());
                itemData.put("quantity", ci.getQuantity());
                itemData.put("lineDiscount", ci.getLineDiscount());
                itemData.put("lineTax", ci.getLineTax());
                itemsData.add(itemData);
            }
            data.put("items", itemsData);
            
            data.put("subtotal", tx.getSubtotal());
            data.put("discountTotal", tx.getDiscountTotal());
            data.put("taxTotal", tx.getTaxTotal());
            data.put("grandTotal", tx.getGrandTotal());
            data.put("receiptPath", tx.getReceiptPath());
            data.put("promoCode", tx.getPromoCode());
            data.put("refunded", tx.isRefunded());
            
            oos.writeObject(data);
        }
    }
    
    public List<Transaction> loadAllTransactions() {
        List<Transaction> transactions = new ArrayList<>();
        
        try {
            Path dir = Paths.get(transactionsDir);
            if (!Files.exists(dir)) {
                return transactions;
            }
            
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.dat")) {
                for (Path filePath : stream) {
                    try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filePath.toFile()))) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> data = (Map<String, Object>) ois.readObject();
                        
                        Transaction tx = convertFromData(data);
                        if (tx != null) {
                            transactions.add(tx);
                        }
                    } catch (Exception e) {
                        System.err.println("Failed to load transaction from " + filePath + ": " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to load transactions: " + e.getMessage());
        }
        
        // Sort by timestamp (newest first)
        transactions.sort((t1, t2) -> t2.getTimestamp().compareTo(t1.getTimestamp()));
        return transactions;
    }
    
    private Transaction convertFromData(Map<String, Object> data) {
        try {
            // Create cashier user
            String username = (String) data.get("cashierUsername");
            String role = (String) data.get("cashierRole");
            User cashier = new User(username, role);
            
            // Create transaction with reflection to set ID and timestamp
            Transaction tx = new Transaction(cashier);
            
            // Set ID using reflection
            java.lang.reflect.Field idField = Transaction.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(tx, data.get("id"));
            
            // Set timestamp using reflection
            java.lang.reflect.Field timestampField = Transaction.class.getDeclaredField("timestamp");
            timestampField.setAccessible(true);
            String timestampStr = (String) data.get("timestamp");
            LocalDateTime timestamp = LocalDateTime.parse(timestampStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            timestampField.set(tx, timestamp);
            
            // Add items
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> itemsData = (List<Map<String, Object>>) data.get("items");
            
            for (Map<String, Object> itemData : itemsData) {
                String barcode = (String) itemData.get("barcode");
                int quantity = (int) itemData.get("quantity");
                double lineDiscount = (double) itemData.get("lineDiscount");
                double lineTax = (double) itemData.get("lineTax");
                
                Product product = inventoryManager.findByBarcode(barcode);
                if (product != null) {
                    CartItem ci = new CartItem(product, quantity);
                    ci.setLineDiscount(lineDiscount);
                    ci.setLineTax(lineTax);
                    tx.getItems().add(ci);
                }
            }
            
            // Set totals and other properties
            tx.setSubtotal((double) data.get("subtotal"));
            tx.setDiscountTotal((double) data.get("discountTotal"));
            tx.setTaxTotal((double) data.get("taxTotal"));
            tx.setGrandTotal((double) data.get("grandTotal"));
            tx.setReceiptPath((String) data.get("receiptPath"));
            tx.setPromoCode((String) data.get("promoCode"));
            tx.setRefunded((boolean) data.get("refunded"));
            
            return tx;
        } catch (Exception e) {
            System.err.println("Failed to convert transaction data: " + e.getMessage());
            return null;
        }
    }
    
    public Transaction findTransactionById(String id) {
        Path filePath = Paths.get(transactionsDir, "tx_" + id + ".dat");
        if (!Files.exists(filePath)) {
            return null;
        }
        
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filePath.toFile()))) {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) ois.readObject();
            return convertFromData(data);
        } catch (Exception e) {
            System.err.println("Failed to find transaction " + id + ": " + e.getMessage());
            return null;
        }
    }
    
    public void updateTransaction(Transaction tx) throws IOException {
        // Just save it again (overwrites the file)
        saveTransaction(tx);
    }
    
    public void deleteAllTransactions() {
        try {
            Path dir = Paths.get(transactionsDir);
            if (Files.exists(dir)) {
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.dat")) {
                    for (Path filePath : stream) {
                        Files.delete(filePath);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to delete transactions: " + e.getMessage());
        }
    }
}
