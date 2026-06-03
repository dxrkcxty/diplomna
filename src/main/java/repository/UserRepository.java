package repository;

import model.User;
import util.DatabaseConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserRepository {
    
    public List<User> findAll() {
        List<User> users = new ArrayList<>();
        String sqlWithPhone = "SELECT id, email, password, role, first_name, last_name, phone, bonus_balance, bonus_rate, spin_credits FROM users ORDER BY id";
        String sqlWithoutPhone = "SELECT id, email, password, role, first_name, last_name, bonus_balance, bonus_rate, spin_credits FROM users ORDER BY id";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sqlWithPhone)) {
            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }
        } catch (SQLException e) {
            System.err.println("Помилка отримання користувачів (with phone): " + e.getMessage());
            try (Connection conn = DatabaseConnection.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sqlWithoutPhone)) {
                while (rs.next()) {
                    User u = mapResultSetToUserWithoutPhone(rs);
                    users.add(u);
                }
            } catch (SQLException ex) {
                System.err.println("Помилка отримання користувачів: " + ex.getMessage());
            }
        }
        return users;
    }

    public Map<Long, User> findByIds(Collection<Long> ids) {
        Map<Long, User> users = new HashMap<>();
        if (ids == null || ids.isEmpty()) {
            return users;
        }
        List<Long> idList = ids.stream().distinct().filter(id -> id != null && id > 0).toList();
        if (idList.isEmpty()) {
            return users;
        }
        String placeholders = String.join(",", java.util.Collections.nCopies(idList.size(), "?"));
        String sqlWithPhone = "SELECT id, email, password, role, first_name, last_name, phone, bonus_balance, bonus_rate, spin_credits FROM users WHERE id IN (" + placeholders + ")";
        String sqlWithoutPhone = "SELECT id, email, password, role, first_name, last_name, bonus_balance, bonus_rate, spin_credits FROM users WHERE id IN (" + placeholders + ")";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sqlWithPhone)) {
            for (int i = 0; i < idList.size(); i++) {
                pstmt.setLong(i + 1, idList.get(i));
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    User user = mapResultSetToUser(rs);
                    users.put(user.getId(), user);
                }
            }
        } catch (SQLException e) {
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sqlWithoutPhone)) {
                for (int i = 0; i < idList.size(); i++) {
                    pstmt.setLong(i + 1, idList.get(i));
                }
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        User user = mapResultSetToUserWithoutPhone(rs);
                        users.put(user.getId(), user);
                    }
                }
            } catch (SQLException ex) {
                System.err.println("Помилка batch-пошуку користувачів: " + ex.getMessage());
            }
        }
        return users;
    }

    public User findById(long id) {
        String sqlWithPhone = "SELECT id, email, password, role, first_name, last_name, phone, bonus_balance, bonus_rate, spin_credits FROM users WHERE id = ?";
        String sqlWithoutPhone = "SELECT id, email, password, role, first_name, last_name, bonus_balance, bonus_rate, spin_credits FROM users WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sqlWithPhone)) {
            pstmt.setLong(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUser(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Помилка пошуку користувача (with phone): " + e.getMessage());
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sqlWithoutPhone)) {
                pstmt.setLong(1, id);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return mapResultSetToUserWithoutPhone(rs);
                    }
                }
            } catch (SQLException ex) {
                System.err.println("Помилка пошуку користувача: " + ex.getMessage());
            }
        }
        return null;
    }

    public User findByEmail(String email) {
        String sqlWithPhone = "SELECT id, email, password, role, first_name, last_name, phone, bonus_balance, bonus_rate, spin_credits FROM users WHERE email = ?";
        String sqlWithoutPhone = "SELECT id, email, password, role, first_name, last_name, bonus_balance, bonus_rate, spin_credits FROM users WHERE email = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sqlWithPhone)) {
            pstmt.setString(1, email);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUser(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Помилка пошуку користувача за email (with phone): " + e.getMessage());
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sqlWithoutPhone)) {
                pstmt.setString(1, email);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return mapResultSetToUserWithoutPhone(rs);
                    }
                }
            } catch (SQLException ex) {
                System.err.println("Помилка пошуку користувача за email: " + ex.getMessage());
            }
        }
        return null;
    }

    public boolean existsByEmail(String email) {
        String sql = "SELECT COUNT(*) FROM users WHERE email = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, email);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("Помилка перевірки email: " + e.getMessage());
        }
        return false;
    }

    public User save(User user) {
        if (user.getId() == 0) {
            return insert(user);
        } else {
            return update(user);
        }
    }

    private User insert(User user) {
        String sqlWithPhone = "INSERT INTO users (email, password, role, first_name, last_name, phone, bonus_balance, bonus_rate, spin_credits) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";
        String sqlWithoutPhone = "INSERT INTO users (email, password, role, first_name, last_name, bonus_balance, bonus_rate, spin_credits) VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sqlWithPhone)) {
            pstmt.setString(1, user.getEmail());
            pstmt.setString(2, user.getPassword());
            pstmt.setString(3, user.getRole());
            pstmt.setString(4, user.getFirstName());
            pstmt.setString(5, user.getLastName());
            pstmt.setString(6, user.getPhone());
            pstmt.setDouble(7, user.getBonusBalance());
            if (user.getBonusRate() == null) pstmt.setNull(8, Types.INTEGER);
            else pstmt.setInt(8, user.getBonusRate());
            pstmt.setInt(9, user.getSpinCredits());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    user.setId(rs.getLong("id"));
                    return user;
                }
            }
        } catch (SQLException e) {
            System.err.println("Помилка створення користувача (with phone): " + e.getMessage());
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sqlWithoutPhone)) {
                pstmt.setString(1, user.getEmail());
                pstmt.setString(2, user.getPassword());
                pstmt.setString(3, user.getRole());
                pstmt.setString(4, user.getFirstName());
                pstmt.setString(5, user.getLastName());
                pstmt.setDouble(6, user.getBonusBalance());
                if (user.getBonusRate() == null) pstmt.setNull(7, Types.INTEGER);
                else pstmt.setInt(7, user.getBonusRate());
                pstmt.setInt(8, user.getSpinCredits());
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        user.setId(rs.getLong("id"));
                        return user;
                    }
                }
            } catch (SQLException ex) {
                System.err.println("Помилка створення користувача: " + ex.getMessage());
                throw new RuntimeException("Не вдалося створити користувача", ex);
            }
        }
        return null;
    }

    private User update(User user) {
        String sqlWithPhone = "UPDATE users SET email = ?, password = ?, role = ?, first_name = ?, last_name = ?, phone = ?, bonus_balance = ?, bonus_rate = ?, spin_credits = ? WHERE id = ?";
        String sqlWithoutPhone = "UPDATE users SET email = ?, password = ?, role = ?, first_name = ?, last_name = ?, bonus_balance = ?, bonus_rate = ?, spin_credits = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sqlWithPhone)) {
            pstmt.setString(1, user.getEmail());
            pstmt.setString(2, user.getPassword());
            pstmt.setString(3, user.getRole());
            pstmt.setString(4, user.getFirstName());
            pstmt.setString(5, user.getLastName());
            pstmt.setString(6, user.getPhone());
            pstmt.setDouble(7, user.getBonusBalance());
            if (user.getBonusRate() == null) pstmt.setNull(8, Types.INTEGER);
            else pstmt.setInt(8, user.getBonusRate());
            pstmt.setInt(9, user.getSpinCredits());
            pstmt.setLong(10, user.getId());
            int rows = pstmt.executeUpdate();
            if (rows > 0) {
                return user;
            }
        } catch (SQLException e) {
            System.err.println("Помилка оновлення користувача (with phone): " + e.getMessage());
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sqlWithoutPhone)) {
                pstmt.setString(1, user.getEmail());
                pstmt.setString(2, user.getPassword());
                pstmt.setString(3, user.getRole());
                pstmt.setString(4, user.getFirstName());
                pstmt.setString(5, user.getLastName());
                pstmt.setDouble(6, user.getBonusBalance());
                if (user.getBonusRate() == null) pstmt.setNull(7, Types.INTEGER);
                else pstmt.setInt(7, user.getBonusRate());
                pstmt.setInt(8, user.getSpinCredits());
                pstmt.setLong(9, user.getId());
                int rows = pstmt.executeUpdate();
                if (rows > 0) {
                    return user;
                }
            } catch (SQLException ex) {
                System.err.println("Помилка оновлення користувача: " + ex.getMessage());
                throw new RuntimeException("Не вдалося оновити користувача", ex);
            }
        }
        return null;
    }

    public boolean deleteById(long id) {
        String sql = "DELETE FROM users WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Помилка видалення користувача: " + e.getMessage());
        }
        return false;
    }

    public void setPasswordResetCode(long userId, String code, String expiresAt) {
        String sql = "UPDATE users SET password_reset_code = ?, password_reset_expires = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, code);
            pstmt.setString(2, expiresAt);
            pstmt.setLong(3, userId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Помилка збереження reset-коду: " + e.getMessage());
        }
    }

    public void clearPasswordResetCode(long userId) {
        String sql = "UPDATE users SET password_reset_code = NULL, password_reset_expires = NULL WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Помилка очищення reset-коду: " + e.getMessage());
        }
    }

    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getLong("id"));
        user.setEmail(rs.getString("email"));
        user.setPassword(rs.getString("password"));
        user.setRole(rs.getString("role"));
        user.setFirstName(rs.getString("first_name"));
        user.setLastName(rs.getString("last_name"));
        user.setPhone(rs.getString("phone"));
        user.setBonusBalance(rs.getDouble("bonus_balance"));
        int rate = rs.getInt("bonus_rate");
        user.setBonusRate(rs.wasNull() ? null : rate);
        user.setSpinCredits(rs.getInt("spin_credits"));
        return user;
    }

    private User mapResultSetToUserWithoutPhone(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getLong("id"));
        user.setEmail(rs.getString("email"));
        user.setPassword(rs.getString("password"));
        user.setRole(rs.getString("role"));
        user.setFirstName(rs.getString("first_name"));
        user.setLastName(rs.getString("last_name"));
        user.setPhone(null);
        user.setBonusBalance(rs.getDouble("bonus_balance"));
        int rate = rs.getInt("bonus_rate");
        user.setBonusRate(rs.wasNull() ? null : rate);
        user.setSpinCredits(rs.getInt("spin_credits"));
        return user;
    }
}

