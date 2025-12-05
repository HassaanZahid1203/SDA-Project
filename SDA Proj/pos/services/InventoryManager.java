package pos.services;

import pos.models.Product;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

public class InventoryManager {
    private final String inventoryPath;
    private final Map<String, Product> productsByBarcode = new HashMap<>();
    public Collection<Product> getAllProducts() { return productsByBarcode.values(); }

    public InventoryManager(String inventoryPath) {
        this.inventoryPath = inventoryPath;
    }

    public void ensureInventoryFile() throws IOException {
        Path p = Paths.get(inventoryPath);
        if (!Files.exists(p)) {
            StringBuilder sample = new StringBuilder();
            sample.append("barcode\t\tname\t\tprice\t\tstock\t\tlowStockThreshold\t\ttaxRate\n");

            // Add many items
            sample.append("1001\t\tMilk 1L\t\t250.00\t\t30\t\t5\t\t0.10\n");
            sample.append("1002\t\tBread Loaf\t\t120.00\t\t50\t\t10\t\t0.05\n");
            sample.append("1003\t\tEggs Dozen\t\t450.00\t\t40\t\t5\t\t0.00\n");
            sample.append("1004\t\tRice 5kg\t\t1500.00\t\t20\t\t3\t\t0.08\n");
            sample.append("1005\t\tSugar 1kg\t\t180.00\t\t100\t\t20\t\t0.08\n");
            sample.append("1006\t\tTea Pack\t\t600.00\t\t25\t\t5\t\t0.10\n");
            sample.append("1007\t\tCoffee Jar\t\t950.00\t\t15\t\t3\t\t0.10\n");
            sample.append("1008\t\tCooking Oil 5L\t\t2200.00\t\t10\t\t2\t\t0.12\n");
            sample.append("1009\t\tButter 250g\t\t350.00\t\t30\t\t5\t\t0.05\n");
            sample.append("1010\t\tCheese 500g\t\t800.00\t\t20\t\t4\t\t0.05\n");
            sample.append("1011\t\tShampoo Bottle\t\t550.00\t\t40\t\t10\t\t0.15\n");
            sample.append("1012\t\tSoap Bar\t\t120.00\t\t200\t\t30\t\t0.05\n");
            sample.append("1013\t\tToothpaste\t\t250.00\t\t60\t\t10\t\t0.08\n");
            sample.append("1014\t\tDetergent 1kg\t\t400.00\t\t50\t\t10\t\t0.10\n");
            sample.append("1015\t\tSoft Drink 1.5L\t\t180.00\t\t80\t\t15\t\t0.12\n");
            sample.append("1016\t\tBiscuits Pack\t\t90.00\t\t150\t\t20\t\t0.05\n");
            sample.append("1017\t\tChips Packet\t\t70.00\t\t200\t\t30\t\t0.05\n");
            sample.append("1018\t\tChocolate Bar\t\t150.00\t\t100\t\t20\t\t0.08\n");
            sample.append("1019\t\tJuice 1L\t\t220.00\t\t60\t\t10\t\t0.10\n");
            sample.append("1020\t\tWater Bottle 1.5L\t\t60.00\t\t300\t\t50\t\t0.00\n");

            Files.write(p, sample.toString().getBytes());
            System.out.println("Created stock file with sample items at " + p.toAbsolutePath());
        }
    }


    public void load() throws IOException {
        try (BufferedReader br = Files.newBufferedReader(Paths.get(inventoryPath))) {
            String header = br.readLine(); // skip headers
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;

                // 🔹 split by two tabs
                String[] parts = line.split("\\t\\t");  
                if (parts.length < 6) continue;

                String barcode = parts[0].trim();
                String name = parts[1].trim();
                double price = Double.parseDouble(parts[2].trim());
                int stock = Integer.parseInt(parts[3].trim());
                int lst = Integer.parseInt(parts[4].trim());
                double tax = Double.parseDouble(parts[5].trim());

                Product p = new Product(barcode, name, price, stock, lst, tax);
                productsByBarcode.put(barcode, p);
            }
        }
        System.out.println("Inventory loaded: " + productsByBarcode.size() + " products.");
    }



    public void save() throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("barcode\t\tname\t\tprice\t\tstock\t\tlowStockThreshold\t\ttaxRate");
        for (Product p : productsByBarcode.values()) {
            lines.add(
                p.getBarcode() + "\t\t" +
                p.getName() + "\t\t" +
                String.format(Locale.US, "%.2f", p.getPrice()) + "\t\t" +
                p.getStock() + "\t\t" +
                p.getLowStockThreshold() + "\t\t" +
                String.format(Locale.US, "%.2f", p.getTaxRate())
            );
        }
        Files.write(Paths.get(inventoryPath), lines);
    }


    public Product findByBarcode(String barcode) {
        return productsByBarcode.get(barcode);
    }

    public Product findByName(String name) {
        return productsByBarcode.values().stream()
                .filter(p -> p.getName().equalsIgnoreCase(name))
                .findFirst().orElse(null);
    }

    public Product findByQuery(String q) {
        Product byBarcode = findByBarcode(q);
        if (byBarcode != null) return byBarcode;
        return productsByBarcode.values().stream()
                .filter(p -> p.getName().toLowerCase().contains(q.toLowerCase()))
                .findFirst().orElse(null);
    }

    public boolean reserveStock(String barcode, int qty) {
        Product p = productsByBarcode.get(barcode);
        if (p == null || qty <= 0) return false;
        if (p.getStock() < qty) return false;
        p.setStock(p.getStock() - qty);
        return true;
    }

    public void releaseStock(String barcode, int qty) {
        Product p = productsByBarcode.get(barcode);
        if (p != null && qty > 0) {
            p.setStock(p.getStock() + qty);
        }
    }

    public void printLowStockAlerts() {
        List<Product> low = productsByBarcode.values().stream()
                .filter(p -> p.getStock() <= p.getLowStockThreshold())
                .collect(Collectors.toList());
        if (low.isEmpty()) {
            System.out.println("No low stock items.");
        } else {
            System.out.println("Low stock alerts:");
            for (Product p : low) {
                System.out.println(p.getBarcode() + " | " + p.getName() + " | Stock: " + p.getStock());
            }
        }
    }
}
