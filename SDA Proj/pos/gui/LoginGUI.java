package pos.gui;

import pos.models.User;
import pos.services.UserService;

import javax.swing.*;
import java.awt.*;

public class LoginGUI extends JFrame {
    private final UserService userService;

    public LoginGUI() {
        super("POS Login");
        userService = new UserService();
        initUI();
    }

    private void initUI() {
        setLayout(new GridLayout(3, 2, 8, 8));

        JTextField username = new JTextField();
        JPasswordField password = new JPasswordField();
        JButton loginBtn = new JButton("Login");

        add(new JLabel("Username:"));
        add(username);
        add(new JLabel("Password:"));
        add(password);
        add(new JLabel(""));
        add(loginBtn);

        loginBtn.addActionListener(e -> {
            User u = userService.authenticate(username.getText(), new String(password.getPassword()));
            if (u != null) {
                JOptionPane.showMessageDialog(this, "Welcome " + u.getRole());
                try {
                    new CheckoutGUI(u).setVisible(true);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Error opening Checkout: " + ex.getMessage());
                }
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Invalid credentials");
            }
        });

        setSize(300, 150);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LoginGUI().setVisible(true));
    }
}
