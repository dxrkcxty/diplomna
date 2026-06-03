package repository;

import model.Category;
import util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CategoryRepository {

    public List<Category> findAll() {
        List<Category> list = new ArrayList<>();
        String sql = "SELECT id, name FROM categories ORDER BY id";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("Помилка отримання категорій: " + e.getMessage());
        }
        return list;
    }

    public Category findById(long id) {
        String sql = "SELECT id, name FROM categories WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Помилка пошуку категорії: " + e.getMessage());
        }
        return null;
    }

    public Category findByName(String name) {
        String sql = "SELECT id, name FROM categories WHERE LOWER(name) = LOWER(?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Помилка пошуку категорії за назвою: " + e.getMessage());
        }
        return null;
    }

    public long count() {
        String sql = "SELECT COUNT(*) FROM categories";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            System.err.println("Помилка підрахунку категорій: " + e.getMessage());
        }
        return 0;
    }

    public Category insert(String name) {
        String sql = "INSERT INTO categories (name) VALUES (?) RETURNING id";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new Category(rs.getLong("id"), name, List.of());
                }
            }
        } catch (SQLException e) {
            System.err.println("Помилка створення категорії: " + e.getMessage());
            throw new RuntimeException("Не вдалося створити категорію", e);
        }
        return null;
    }

    private Category mapRow(ResultSet rs) throws SQLException {
        return new Category(rs.getLong("id"), rs.getString("name"), List.of());
    }
}
