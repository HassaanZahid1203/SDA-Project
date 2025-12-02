package pos.gui;

import pos.services.ReportService;

import javax.swing.*;
import java.awt.*;

public class ReportsGUI extends JFrame {
    private final ReportService reportService;
    private JTextArea outputArea;

    public ReportsGUI(ReportService reportService) {
        super("Reports");
        this.reportService = reportService;
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout());

        JPanel buttonsPanel = new JPanel(new GridLayout(3, 2, 8, 8));
        buttonsPanel.setBorder(BorderFactory.createTitledBorder("Generate Reports"));

        JButton dailyBtn = new JButton("Daily");
        JButton weeklyBtn = new JButton("Weekly");
        JButton monthlyBtn = new JButton("Monthly");
        JButton cashierBtn = new JButton("Cashier-wise");
        JButton taxBtn = new JButton("Tax & Financial");
        JButton perfBtn = new JButton("Product Performance");

        buttonsPanel.add(dailyBtn); buttonsPanel.add(weeklyBtn);
        buttonsPanel.add(monthlyBtn); buttonsPanel.add(cashierBtn);
        buttonsPanel.add(taxBtn); buttonsPanel.add(perfBtn);

        outputArea = new JTextArea();
        outputArea.setEditable(false);

        add(buttonsPanel, BorderLayout.NORTH);
        add(new JScrollPane(outputArea), BorderLayout.CENTER);

        dailyBtn.addActionListener(e -> runReport("1"));
        weeklyBtn.addActionListener(e -> runReport("2"));
        monthlyBtn.addActionListener(e -> runReport("3"));
        cashierBtn.addActionListener(e -> runReport("4"));
        taxBtn.addActionListener(e -> runReport("5"));
        perfBtn.addActionListener(e -> runReport("6"));

        setSize(600, 400);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    }

    private void runReport(String code) {
        String report = reportService.generate(code);
        outputArea.setText(report);  
    }

}
