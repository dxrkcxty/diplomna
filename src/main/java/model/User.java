package model;

import java.util.List;

public class User {
    private long id;
    private String email;
    private String password;
    private String role;
    private String firstName;
    private String lastName;
    private String phone;
    private double bonusBalance;
    private Integer bonusRate;
    private int spinCredits;
    private List<Order> orders;

    public User() {}
    public User(long id, String email, String password, String role, List<Order> orders) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.role = role;
        this.orders = orders;
    }
    public User(long id, String email, String password, String role, String firstName, String lastName, List<Order> orders) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.role = role;
        this.firstName = firstName;
        this.lastName = lastName;
        this.orders = orders;
    }
    public User(long id, String email, String password, String role, String firstName, String lastName, double bonusBalance, Integer bonusRate, List<Order> orders) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.role = role;
        this.firstName = firstName;
        this.lastName = lastName;
        this.bonusBalance = bonusBalance;
        this.bonusRate = bonusRate;
        this.spinCredits = 0;
        this.orders = orders;
    }
    public User(long id, String email, String password, String role, String firstName, String lastName, double bonusBalance, Integer bonusRate, int spinCredits, List<Order> orders) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.role = role;
        this.firstName = firstName;
        this.lastName = lastName;
        this.bonusBalance = bonusBalance;
        this.bonusRate = bonusRate;
        this.spinCredits = spinCredits;
        this.orders = orders;
    }
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public double getBonusBalance() { return bonusBalance; }
    public void setBonusBalance(double bonusBalance) { this.bonusBalance = bonusBalance; }
    public Integer getBonusRate() { return bonusRate; }
    public void setBonusRate(Integer bonusRate) { this.bonusRate = bonusRate; }
    public int getSpinCredits() { return spinCredits; }
    public void setSpinCredits(int spinCredits) { this.spinCredits = spinCredits; }
    public List<Order> getOrders() { return orders; }
    public void setOrders(List<Order> orders) { this.orders = orders; }
}
