package pos.services;

import pos.models.Customer;

import java.util.*;
import java.util.stream.Collectors;

public class CustomerService {
    private final Map<String, Customer> byPhone = new HashMap<>();
    private final Map<String, Customer> byId = new HashMap<>();

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
        return c;
    }

    public Customer attachOrCreate(String phone) {
        Customer c = byPhone.get(phone);
        if (c == null) {
            c = new Customer(phone, phone, phone);
            byPhone.put(phone, c);
            byId.put(c.getId(), c);
        }
        return c;
    }

    public List<Customer> search(String query) {
        String q = query.toLowerCase();
        return byId.values().stream()
                .filter(c -> c.getId().toLowerCase().contains(q)
                        || c.getName().toLowerCase().contains(q)
                        || c.getPhone().toLowerCase().contains(q))
                .collect(Collectors.toList());
    }
}
