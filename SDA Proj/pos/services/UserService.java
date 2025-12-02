package pos.services;

import pos.models.User;

import java.util.HashMap;
import java.util.Map;

public class UserService {

    // username → password
    private final Map<String, String> credentials = new HashMap<>();
    
    // username → role
    private final Map<String, String> roles = new HashMap<>();

    public UserService() {
        // === Add your real users here ===
        
        // Admin
        addUser("Hassaan", "1234", "Admin");       // Hassaan is Admin
        
        // Managers & Cashiers
        addUser("Asad",    "asad123", "Manager");
        addUser("Ali",     "ali123",  "Manager");
        addUser("Eesha",   "eesha123","Cashier");
        
        // You can easily add more anytime:
        // addUser("username", "password", "Admin/Manager/Cashier");
    }

    private void addUser(String username, String password, String role) {
        credentials.put(username.toLowerCase(), password);  // case-insensitive login
        roles.put(username.toLowerCase(), role);
    }

    public User authenticate(String username, String password) {
        if (username == null || password == null) return null;

        String storedPass = credentials.get(username.toLowerCase());
        if (storedPass != null && storedPass.equals(password)) {
            String role = roles.get(username.toLowerCase());
            return new User(username, role);
        }
        return null;
    }

    // Optional: helpful for future features
    public boolean isAdmin(User user) {
        return user != null && "Admin".equalsIgnoreCase(user.getRole());
    }

    public boolean isManagerOrAdmin(User user) {
        if (user == null) return false;
        String role = user.getRole();
        return "Admin".equalsIgnoreCase(role) || "Manager".equalsIgnoreCase(role);
    }
}