package service;

import model.Product;
import model.Category;
import java.util.List;

public class ProductServiceTest {
    public static void main(String[] args) {
        ProductService service = new ProductService();
        Product p1 = new Product(0,"Apple",12.0,null,null);
        Product created = service.create(p1);
        assert created.getId() > 0 : "Create product failed";

        List<Product> all = service.getAll();
        assert all.size() == 1 : "getAll failed";
        
        Product updated = service.update(created.getId(), new Product(0,"Orange",13.5,null,null));
        assert updated.getName().equals("Orange") : "Update name failed";

        boolean removed = service.delete(created.getId());
        assert removed : "Delete failed";
    }
}
