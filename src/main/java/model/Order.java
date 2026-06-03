package model;

import java.util.List;

public class Order {
    private long id;
    private User user;
    private List<Product> products;
    private String date;
    private String status;
    private String fullName;
    private String phone;
    private String deliveryAddress;
    private String deliveryCarrier;
    private String deliveryBranch;
    private String paymentMethod;
    private String paymentStatus;
    private double totalAmount;
    private double bonusUsed;
    private double bonusEarned;
    private Integer bonusRate;
    private String itemsSnapshot;

    public Order() {}
    public Order(long id, User user, List<Product> products, String date, String status) {
        this.id = id;
        this.user = user;
        this.products = products;
        this.date = date;
        this.status = status;
    }
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public List<Product> getProducts() { return products; }
    public void setProducts(List<Product> products) { this.products = products; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(String deliveryAddress) { this.deliveryAddress = deliveryAddress; }
    public String getDeliveryCarrier() { return deliveryCarrier; }
    public void setDeliveryCarrier(String deliveryCarrier) { this.deliveryCarrier = deliveryCarrier; }
    public String getDeliveryBranch() { return deliveryBranch; }
    public void setDeliveryBranch(String deliveryBranch) { this.deliveryBranch = deliveryBranch; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }
    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }
    public double getBonusUsed() { return bonusUsed; }
    public void setBonusUsed(double bonusUsed) { this.bonusUsed = bonusUsed; }
    public double getBonusEarned() { return bonusEarned; }
    public void setBonusEarned(double bonusEarned) { this.bonusEarned = bonusEarned; }
    public Integer getBonusRate() { return bonusRate; }
    public void setBonusRate(Integer bonusRate) { this.bonusRate = bonusRate; }
    public String getItemsSnapshot() { return itemsSnapshot; }
    public void setItemsSnapshot(String itemsSnapshot) { this.itemsSnapshot = itemsSnapshot; }
}
