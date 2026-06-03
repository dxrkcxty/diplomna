package model;

public class Review {
    private long id;
    private User user;
    private Product product;
    private String comment;
    private int rating;

    public Review() {}
    public Review(long id, User user, Product product, String comment, int rating) {
        this.id = id;
        this.user = user;
        this.product = product;
        this.comment = comment;
        this.rating = rating;
    }
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }
}
