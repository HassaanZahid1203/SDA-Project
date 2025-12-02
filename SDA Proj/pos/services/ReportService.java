package pos.services;

import pos.models.*;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

public class ReportService {

    private final POSService posService;

    public ReportService(POSService posService) {
        this.posService = posService;
    }

    public String generate(String code) {
        return switch (code) {
            case "1" -> generateDaily();
            case "2" -> generateWeekly();
            case "3" -> generateMonthly();
            case "4" -> generateCashierReport();
            case "5" -> generateTaxReport();
            case "6" -> generateProductPerformance();
            default -> "Invalid report code.";
        };
    }

    private String generateDaily() {
        LocalDate today = LocalDate.now();
        var txs = getTransactionsAfter(today.atStartOfDay());

        StringBuilder sb = new StringBuilder();
        sb.append("=== DAILY REPORT - ").append(today).append(" ===\n");
        sb.append("Transactions Today: ").append(txs.size()).append("\n");
        sb.append("Total Sales: PKR ").append(String.format("%.2f", txs.stream().mapToDouble(Transaction::getGrandTotal).sum())).append("\n");
        sb.append("Average Transaction: PKR ").append(txs.isEmpty() ? "0.00" : String.format("%.2f", txs.stream().mapToDouble(Transaction::getGrandTotal).average().orElse(0))).append("\n");
        return sb.toString();
    }

    private String generateWeekly() {
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        var txs = getTransactionsAfter(weekAgo);

        StringBuilder sb = new StringBuilder();
        sb.append("=== WEEKLY REPORT (Last 7 Days) ===\n");
        sb.append("Period: ").append(weekAgo.toLocalDate()).append(" to ").append(LocalDate.now()).append("\n");
        sb.append("Total Transactions: ").append(txs.size()).append("\n");
        sb.append("Total Revenue: PKR ").append(String.format("%.2f", txs.stream().mapToDouble(Transaction::getGrandTotal).sum())).append("\n");
        return sb.toString();
    }

    private String generateMonthly() {
        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        var txs = getTransactionsAfter(monthStart);

        StringBuilder sb = new StringBuilder();
        sb.append("=== MONTHLY REPORT - ").append(monthStart.getMonth()).append(" ").append(monthStart.getYear()).append(" ===\n");
        sb.append("Total Sales: PKR ").append(String.format("%.2f", txs.stream().mapToDouble(Transaction::getGrandTotal).sum())).append("\n");
        sb.append("Transactions: ").append(txs.size()).append("\n");
        return sb.toString();
    }

    private String generateCashierReport() {
        Map<String, List<Transaction>> byCashier = getAllTransactions().stream()
                .collect(Collectors.groupingBy(tx -> tx.getCashier().getUsername()));

        StringBuilder sb = new StringBuilder("=== CASHIER-WISE REPORT ===\n");
        byCashier.forEach((name, list) -> {
            double total = list.stream().mapToDouble(Transaction::getGrandTotal).sum();
            sb.append(name).append(" (").append(list.get(0).getCashier().getRole()).append(")")
              .append(": PKR ").append(String.format("%.2f", total))
              .append(" (").append(list.size()).append(" sales)\n");
        });
        return sb.toString();
    }

    private String generateTaxReport() {
        var txs = getAllTransactions();
        double totalTax = txs.stream().mapToDouble(Transaction::getTaxTotal).sum();
        double totalDiscount = txs.stream().mapToDouble(Transaction::getDiscountTotal).sum();

        StringBuilder sb = new StringBuilder("=== TAX & FINANCIAL SUMMARY ===\n");
        sb.append("Total Tax Collected: PKR ").append(String.format("%.2f", totalTax)).append("\n");
        sb.append("Total Discounts Given: PKR ").append(String.format("%.2f", totalDiscount)).append("\n");
        sb.append("Net Revenue (after discount): PKR ").append(String.format("%.2f", txs.stream().mapToDouble(t -> t.getGrandTotal() - t.getTaxTotal()).sum())).append("\n");
        return sb.toString();
    }

    private String generateProductPerformance() {
        Map<String, Integer> salesCount = new HashMap<>();
        Map<String, Double> revenue = new HashMap<>();

        getAllTransactions().forEach(tx -> {
            tx.getItems().forEach(ci -> {
                String name = ci.getProduct().getName();
                int qty = ci.getQuantity();
                double lineTotal = ci.getLineSubtotal();

                salesCount.put(name, salesCount.getOrDefault(name, 0) + qty);
                revenue.put(name, revenue.getOrDefault(name, 0.0) + lineTotal);
            });
        });

        StringBuilder sb = new StringBuilder("=== TOP SELLING PRODUCTS ===\n");
        salesCount.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(10)
                .forEach(e -> {
                    String name = e.getKey();
                    int qty = e.getValue();
                    double rev = revenue.getOrDefault(name, 0.0);
                    sb.append(name).append(": ").append(qty).append(" units → PKR ").append(String.format("%.2f", rev)).append("\n");
                });

        return sb.toString();
    }

    // Helper methods
    private List<Transaction> getAllTransactions() {
        return new ArrayList<>(posService.getAllTransactions().values().stream()
                .filter(tx -> !tx.isRefunded())
                .toList());
    }

    private List<Transaction> getTransactionsAfter(LocalDateTime time) {
        return getAllTransactions().stream()
                .filter(tx -> tx.getTimestamp().isAfter(time))
                .toList();
    }
}