package pos.services;

import pos.models.User;

import java.util.*;

public class UserService {
    private final Map<String, String> credentials = new HashMap<>();
    private final Map<String, String> roles = new HashMap<>();

    public UserService() {
        // demo users
        credentials.put("admin", "1234");
        roles.put("admin", "Admin");

        credentials.put("cashier1", "1111");
        roles.put("cashier1", "Cashier");

        credentials.put("manager", "2222");
        roles.put("manager", "Manager");
    }

    public User authenticate(String username, String pass) {
        String expected = credentials.get(username);
        if (expected != null && expected.equals(pass)) {
            return new User(username, roles.get(username));
        }
        return null;
    }

    public boolean canPerform(User user, String action) {
        String role = user.getRole();
        switch (action) {
            case "sale":
                return role.equals("Cashier") || role.equals("Admin") || role.equals("Manager");
            case "refund":
                return role.equals("Admin") || role.equals("Manager");
            case "void":
                return role.equals("Admin") || role.equals("Manager");
            default:
                return false;
        }
    }

    public void logActivity(User user, String activity) {
        System.out.println("[AUDIT] " + user.getUsername() + " - " + activity);
    }
}
