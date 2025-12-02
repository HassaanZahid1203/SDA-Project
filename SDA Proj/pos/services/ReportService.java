package pos.services;

public class ReportService {

    // Stubs: plug in to your data store or persist transactions to CSV/DB for real reports
    public void generate(String choice) {
        switch (choice) {
            case "1": System.out.println("Daily report generated (stub)."); break;
            case "2": System.out.println("Weekly report generated (stub)."); break;
            case "3": System.out.println("Monthly report generated (stub)."); break;
            case "4": System.out.println("Cashier-wise report generated (stub)."); break;
            case "5": System.out.println("Tax & financial report generated (stub)."); break;
            case "6": System.out.println("Product performance report generated (stub)."); break;
            default: System.out.println("Invalid report choice.");
        }
    }
}
