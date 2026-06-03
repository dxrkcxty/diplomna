package repository;

import model.OrderMessage;
import util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class OrderMessageRepository {

    public List<OrderMessage> findByOrderId(long orderId) {
        List<OrderMessage> messages = new ArrayList<>();
        String sql = "SELECT id, order_id, sender_email, sender_role, message, created_at FROM order_messages WHERE order_id = ? ORDER BY id";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, orderId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    OrderMessage msg = new OrderMessage();
                    msg.setId(rs.getLong("id"));
                    msg.setOrderId(rs.getLong("order_id"));
                    msg.setSenderEmail(rs.getString("sender_email"));
                    msg.setSenderRole(rs.getString("sender_role"));
                    msg.setMessage(rs.getString("message"));
                    msg.setCreatedAt(rs.getString("created_at"));
                    messages.add(msg);
                }
            }
        } catch (SQLException e) {
            System.err.println("Помилка отримання повідомлень: " + e.getMessage());
        }
        return messages;
    }

    public OrderMessage insert(OrderMessage message) {
        String sql = "INSERT INTO order_messages (order_id, sender_email, sender_role, message, created_at) VALUES (?, ?, ?, ?, ?) RETURNING id";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, message.getOrderId());
            pstmt.setString(2, message.getSenderEmail());
            pstmt.setString(3, message.getSenderRole());
            pstmt.setString(4, message.getMessage());
            pstmt.setString(5, message.getCreatedAt());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    message.setId(rs.getLong("id"));
                    return message;
                }
            }
        } catch (SQLException e) {
            System.err.println("Помилка створення повідомлення: " + e.getMessage());
            throw new RuntimeException("Не вдалося створити повідомлення", e);
        }
        return null;
    }
}

