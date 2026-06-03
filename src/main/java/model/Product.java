package model;

import java.util.List;

public class Product {
    private long id;
    private String name;
    private double price;
    private Category category;
    private List<Review> reviews;
    private String type;
    private String size;
    private String gender;
    private String imageUrl;
    private Double discountPercent;
    private Double discountAmount;

    public Product() {}
    public Product(long id, String name, double price, Category category, List<Review> reviews) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.category = category;
        this.reviews = reviews;
    }
    public Product(long id, String name, double price, Category category, List<Review> reviews, 
                   String type, String size, String gender, String imageUrl) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.category = category;
        this.reviews = reviews;
        this.type = type;
        this.size = size;
        this.gender = gender;
        this.imageUrl = imageUrl;
        this.discountPercent = null;
        this.discountAmount = null;
    }
    
    public Product(long id, String name, double price, Category category, List<Review> reviews, 
                   String type, String size, String gender, String imageUrl, 
                   Double discountPercent, Double discountAmount) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.category = category;
        this.reviews = reviews;
        this.type = type;
        this.size = size;
        this.gender = gender;
        this.imageUrl = imageUrl;
        this.discountPercent = discountPercent;
        this.discountAmount = discountAmount;
    }
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    public List<Review> getReviews() { return reviews; }
    public void setReviews(List<Review> reviews) { this.reviews = reviews; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getSize() { return size; }
    public void setSize(String size) { this.size = size; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public Double getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(Double discountPercent) { this.discountPercent = discountPercent; }
    public Double getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(Double discountAmount) { this.discountAmount = discountAmount; }
    
    public double getFinalPrice() {
        double finalPrice = price;
        if (discountPercent != null && discountPercent > 0) {
            finalPrice = price * (1 - discountPercent / 100.0);
        } else if (discountAmount != null && discountAmount > 0) {
            finalPrice = Math.max(0, price - discountAmount);
        }
        return finalPrice;
    }
    
    public boolean hasDiscount() {
        return (discountPercent != null && discountPercent > 0) || 
               (discountAmount != null && discountAmount > 0);
    }
}
