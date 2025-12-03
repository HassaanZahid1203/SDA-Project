package pos.services;

import pos.models.Customer;
import java.util.List;

public class CustomerService {
    private final CustomerStorage customerStorage;
    
    public CustomerService() {
        this.customerStorage = new CustomerStorage("customers.txt");
    }

    public Customer createOrUpdate(String phone, String name, String contact) {
        return customerStorage.createOrUpdate(phone, name, contact);
    }

    public Customer attachOrCreate(String phone) {
        return customerStorage.attachOrCreate(phone);
    }

    public List<Customer> search(String query) {
        return customerStorage.search(query);
    }

    public void addLoyaltyPoints(String phone, double points) {
        customerStorage.addLoyaltyPoints(phone, points);
    }

    public void redeemLoyaltyPoints(String phone, double points) {
        customerStorage.redeemLoyaltyPoints(phone, points);
    }

    public Customer getByPhone(String phone) {
        return customerStorage.getByPhone(phone);
    }

    public Customer getById(String id) {
        return customerStorage.getById(id);
    }

    public List<Customer> getAllCustomers() {
        return customerStorage.getAllCustomers();
    }

    public void deleteCustomer(String phone) {
        customerStorage.deleteCustomer(phone);
    }
}