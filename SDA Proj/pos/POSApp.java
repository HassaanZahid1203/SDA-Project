package pos;

import pos.gui.LoginGUI;

import javax.swing.*;

public class POSApp {
    public static void main(String[] args) {
        // Always start with the Login screen
        SwingUtilities.invokeLater(() -> {
            new LoginGUI().setVisible(true);
        });
    }
}
