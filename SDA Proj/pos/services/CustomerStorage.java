package pos.services;

import pos.models.Customer;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public class CustomerStorage {
    private final String customersFile;
    private final Map<String, Customer> byPhone = new HashMap<>();
    private final Map<String, Customer> byId = new HashMap<>();

    public CustomerStorage(String customersFile) {
        this.customersFile = customersFile;
        ensureCustomersFile();
        load();
    }

    private void ensureCustomersFile() {
        try {
            Path p = Paths.get(customersFile);
            if (!Files.exists(p)) {
                StringBuilder header = new StringBuilder();
                header.append("phone,name,contact,loyaltyPoints\n");
                header.append("=========================================\n");
                Files.write(p, header.toString().getBytes());
                System.out.println("Created customers file: " + p.toAbsolutePath());
            }
        } catch (IOException e) {
            System.err.println("Failed to create customers file: " + e.getMessage());
        }
    }

    public void load() {
        byPhone.clear();
        byId.clear();
        
        try (BufferedReader br = Files.newBufferedReader(Paths.get(customersFile))) {
            String line;
            boolean firstLine = true;
            
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty() || line.startsWith("=") || firstLine) {
                    firstLine = false;
                    continue;
                }
                
                String[] parts = line.split(",");
                if (parts.length >= 4) {
                    try {
                        String phone = parts[0].trim();
                        String name = parts[1].trim();
                        String contact = parts[2].trim();
                        double loyaltyPoints = Double.parseDouble(parts[3].trim());
                        
                        Customer c = new Customer(phone, name, contact);
                        
                        java.lang.reflect.Field pointsField = Customer.class.getDeclaredField("loyaltyPoints");
                        pointsField.setAccessible(true);
                        pointsField.set(c, loyaltyPoints);
                        
                        byPhone.put(phone, c);
                        byId.put(c.getId(), c);
                        
                    } catch (Exception e) {
                        System.err.println("Failed to parse customer line: " + line);
                    }
                }
            }
            
        } catch (IOException e) {
            System.err.println("Failed to load customers: " + e.getMessage());
        }
    }

    private void saveToFile() {
        try (BufferedWriter bw = Files.newBufferedWriter(Paths.get(customersFile))) {
            bw.write("phone,name,contact,loyaltyPoints\n");
            bw.write("=========================================\n");
            
            for (Customer c : byPhone.values()) {
                bw.write(c.getPhone() + "," + 
                         c.getName() + "," + 
                         c.getContact() + "," + 
                         String.format("%.2f", c.getLoyaltyPoints()));
                bw.newLine();
            }
            
        } catch (IOException e) {
            System.err.println("Failed to save customers: " + e.getMessage());
        }
    }

    public Customer createOrUpdate(String phone, String name, String contact) {
        Customer c = byPhone.get(phone);
        if (c == null) {
            c = new Customer(phone, name, contact);
            byPhone.put(phone, c);
            byId.put(c.getId(), c);
        } else {
            c.setName(name);
            c.setContact(contact);
        }
        saveToFile();
        return c;
    }

    public Customer attachOrCreate(String phone) {
        Customer c = byPhone.get(phone);
        if (c == null) {
            c = new Customer(phone, "Unknown", phone);
            byPhone.put(phone, c);
            byId.put(c.getId(), c);
            saveToFile();
        }
        return c;
    }

    public List<Customer> search(String query) {
        String q = query.toLowerCase();
        List<Customer> results = new ArrayList<>();
        
        for (Customer c : byId.values()) {
            if (c.getId().toLowerCase().contains(q) ||
                c.getName().toLowerCase().contains(q) ||
                c.getPhone().toLowerCase().contains(q) ||
                c.getContact().toLowerCase().contains(q)) {
                results.add(c);
            }
        }
        return results;
    }

    public void addLoyaltyPoints(String phone, double points) {
        Customer c = byPhone.get(phone);
        if (c != null) {
            c.addPoints(points);
            saveToFile();
        }
    }

    public void redeemLoyaltyPoints(String phone, double points) {
        Customer c = byPhone.get(phone);
        if (c != null) {
            c.redeemPoints(points);
            saveToFile();
        }
    }

    public Customer getByPhone(String phone) {
        return byPhone.get(phone);
    }

    public Customer getById(String id) {
        return byId.get(id);
    }

    public List<Customer> getAllCustomers() {
        return new ArrayList<>(byPhone.values());
    }

    public void deleteCustomer(String phone) {
        Customer c = byPhone.remove(phone);
        if (c != null) {
            byId.remove(c.getId());
            saveToFile();
        }
    }
}