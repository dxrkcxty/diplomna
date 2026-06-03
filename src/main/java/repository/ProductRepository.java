package repository;

import model.Category;
import model.Product;
import util.DatabaseConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProductRepository {

    private static final String SELECT_WITH_CATEGORY =
            "SELECT p.id, p.name, p.price, p.category_id, p.type, p.size, p.gender, p.image_url, p.discount_percent, p.discount_amount, "
                    + "c.id AS cat_id, c.name AS cat_name "
                    + "FROM products p LEFT JOIN categories c ON p.category_id = c.id ";

    public List<Product> findAll() {
        List<Product> products = new ArrayList<>();
        String sql = SELECT_WITH_CATEGORY + "ORDER BY p.id";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                products.add(mapResultSetToProduct(rs));
            }
        } catch (SQLException e) {
            System.err.println("Помилка отримання товарів: " + e.getMessage());
        }
        return products;
    }

    public Product findById(long id) {
        String sql = SELECT_WITH_CATEGORY + "WHERE p.id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToProduct(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Помилка пошуку товару: " + e.getMessage());
        }
        return null;
    }

    public List<Product> findByCategoryId(long categoryId) {
        List<Product> products = new ArrayList<>();
        String sql = SELECT_WITH_CATEGORY + "WHERE p.category_id = ? ORDER BY p.id";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, categoryId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    products.add(mapResultSetToProduct(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Помилка пошуку товарів за категорією: " + e.getMessage());
        }
        return products;
    }

    public List<Product> findWithDiscounts() {
        List<Product> products = new ArrayList<>();
        String sql = SELECT_WITH_CATEGORY + "WHERE p.discount_percent > 0 OR p.discount_amount > 0 ORDER BY p.id";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                products.add(mapResultSetToProduct(rs));
            }
        } catch (SQLException e) {
            System.err.println("Помилка отримання товарів зі знижками: " + e.getMessage());
        }
        return products;
    }

    public List<Product> findOrderedByUserId(long userId) {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT DISTINCT p.id, p.name, p.price, p.category_id, p.type, p.size, p.gender, p.image_url, "
                + "p.discount_percent, p.discount_amount, c.id AS cat_id, c.name AS cat_name "
                + "FROM products p "
                + "LEFT JOIN categories c ON p.category_id = c.id "
                + "INNER JOIN order_products op ON op.product_id = p.id "
                + "INNER JOIN orders o ON o.id = op.order_id AND o.user_id = ? "
                + "ORDER BY p.name";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    products.add(mapResultSetToProduct(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Помилка отримання куплених товарів: " + e.getMessage());
        }
        return products;
    }

    public Product save(Product product) {
        if (product.getId() == 0) {
            return insert(product);
        } else {
            return update(product);
        }
    }

    private Product insert(Product product) {
        String sql = "INSERT INTO products (name, price, category_id, type, size, gender, image_url, discount_percent, discount_amount) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, product.getName());
            pstmt.setDouble(2, product.getPrice());
            pstmt.setObject(3, product.getCategory() != null ? product.getCategory().getId() : null, Types.BIGINT);
            pstmt.setString(4, product.getType());
            pstmt.setString(5, product.getSize());
            pstmt.setString(6, product.getGender());
            pstmt.setString(7, product.getImageUrl());
            pstmt.setObject(8, product.getDiscountPercent(), Types.DECIMAL);
            pstmt.setObject(9, product.getDiscountAmount(), Types.DECIMAL);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    product.setId(rs.getLong("id"));
                    return product;
                }
            }
        } catch (SQLException e) {
            System.err.println("Помилка створення товару: " + e.getMessage());
            throw new RuntimeException("Не вдалося створити товар", e);
        }
        return null;
    }

    private Product update(Product product) {
        String sql = "UPDATE products SET name = ?, price = ?, category_id = ?, type = ?, size = ?, gender = ?, image_url = ?, discount_percent = ?, discount_amount = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, product.getName());
            pstmt.setDouble(2, product.getPrice());
            pstmt.setObject(3, product.getCategory() != null ? product.getCategory().getId() : null, Types.BIGINT);
            pstmt.setString(4, product.getType());
            pstmt.setString(5, product.getSize());
            pstmt.setString(6, product.getGender());
            pstmt.setString(7, product.getImageUrl());
            pstmt.setObject(8, product.getDiscountPercent(), Types.DECIMAL);
            pstmt.setObject(9, product.getDiscountAmount(), Types.DECIMAL);
            pstmt.setLong(10, product.getId());
            int rows = pstmt.executeUpdate();
            if (rows > 0) {
                return product;
            }
        } catch (SQLException e) {
            System.err.println("Помилка оновлення товару: " + e.getMessage());
            throw new RuntimeException("Не вдалося оновити товар", e);
        }
        return null;
    }

    public boolean deleteById(long id) {
        String sql = "DELETE FROM products WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Помилка видалення товару: " + e.getMessage());
        }
        return false;
    }

    private Product mapResultSetToProduct(ResultSet rs) throws SQLException {
        Product product = new Product();
        product.setId(rs.getLong("id"));
        product.setName(rs.getString("name"));
        product.setPrice(rs.getDouble("price"));
        product.setType(rs.getString("type"));
        product.setSize(rs.getString("size"));
        product.setGender(rs.getString("gender"));
        product.setImageUrl(rs.getString("image_url"));
        Object discountPercent = rs.getObject("discount_percent");
        if (discountPercent != null) {
            product.setDiscountPercent(((Number) discountPercent).doubleValue());
        }
        Object discountAmount = rs.getObject("discount_amount");
        if (discountAmount != null) {
            product.setDiscountAmount(((Number) discountAmount).doubleValue());
        }
        long catId = rs.getLong("cat_id");
        if (!rs.wasNull() && catId > 0) {
            product.setCategory(new Category(catId, rs.getString("cat_name"), List.of()));
        }
        return product;
    }
}

