package model;

import java.util.List;

public class Category {
    private long id;
    private String name;
    private List<Product> products;

    public Category() {}
    public Category(long id, String name, List<Product> products) {
        this.id = id;
        this.name = name;
        this.products = products;
    }
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<Product> getProducts() { return products; }
    public void setProducts(List<Product> products) { this.products = products; }
}
