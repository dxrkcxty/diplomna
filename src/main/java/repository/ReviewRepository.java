package repository;

import model.Review;
import model.User;
import model.Product;
import util.DatabaseConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReviewRepository {
    private static final String REVIEW_SELECT =
            "SELECT r.id, r.user_id, r.product_id, r.rating, r.comment, "
                    + "u.email AS user_email, p.name AS product_name "
                    + "FROM reviews r "
                    + "LEFT JOIN users u ON u.id = r.user_id "
                    + "LEFT JOIN products p ON p.id = r.product_id ";

    public ReviewRepository() {
    }

    public List<Review> findAll() {
        List<Review> reviews = new ArrayList<>();
        String sql = REVIEW_SELECT + "ORDER BY r.id";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                reviews.add(mapJoinedReview(rs));
            }
        } catch (SQLException e) {
            System.err.println("Помилка отримання відгуків: " + e.getMessage());
        }
        return reviews;
    }

    public Review findById(long id) {
        String sql = REVIEW_SELECT + "WHERE r.id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapJoinedReview(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Помилка пошуку відгуку: " + e.getMessage());
        }
        return null;
    }

    public List<Review> findByProductId(long productId) {
        List<Review> reviews = new ArrayList<>();
        String sql = REVIEW_SELECT + "WHERE r.product_id = ? ORDER BY r.id";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, productId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    reviews.add(mapJoinedReview(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Помилка пошуку відгуків за товаром: " + e.getMessage());
        }
        return reviews;
    }

    public Review save(Review review) {
        if (review.getId() == 0) {
            return insert(review);
        } else {
            return update(review);
        }
    }

    private Review insert(Review review) {
        String sql = "INSERT INTO reviews (user_id, product_id, rating, comment) VALUES (?, ?, ?, ?) RETURNING id";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, review.getUser().getId());
            pstmt.setObject(2, review.getProduct() != null ? review.getProduct().getId() : null, Types.BIGINT);
            pstmt.setInt(3, review.getRating());
            pstmt.setString(4, review.getComment());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    review.setId(rs.getLong("id"));
                    return review;
                }
            }
        } catch (SQLException e) {
            System.err.println("Помилка створення відгуку: " + e.getMessage());
            throw new RuntimeException("Не вдалося створити відгук", e);
        }
        return null;
    }

    private Review update(Review review) {
        String sql = "UPDATE reviews SET user_id = ?, product_id = ?, rating = ?, comment = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, review.getUser().getId());
            pstmt.setObject(2, review.getProduct() != null ? review.getProduct().getId() : null, Types.BIGINT);
            pstmt.setInt(3, review.getRating());
            pstmt.setString(4, review.getComment());
            pstmt.setLong(5, review.getId());
            int rows = pstmt.executeUpdate();
            if (rows > 0) {
                return review;
            }
        } catch (SQLException e) {
            System.err.println("Помилка оновлення відгуку: " + e.getMessage());
            throw new RuntimeException("Не вдалося оновити відгук", e);
        }
        return null;
    }

    public boolean deleteById(long id) {
        String sql = "DELETE FROM reviews WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Помилка видалення відгуку: " + e.getMessage());
        }
        return false;
    }

    private Review mapJoinedReview(ResultSet rs) throws SQLException {
        Review review = new Review();
        review.setId(rs.getLong("id"));
        review.setRating(rs.getInt("rating"));
        review.setComment(rs.getString("comment"));

        User user = new User();
        user.setId(rs.getLong("user_id"));
        try {
            user.setEmail(rs.getString("user_email"));
        } catch (SQLException ignored) {
        }
        review.setUser(user);

        Object productIdObj = rs.getObject("product_id");
        if (productIdObj != null) {
            long productId = ((Number) productIdObj).longValue();
            if (productId > 0) {
                Product product = new Product();
                product.setId(productId);
                try {
                    product.setName(rs.getString("product_name"));
                } catch (SQLException ignored) {
                }
                review.setProduct(product);
            }
        }
        return review;
    }
}
