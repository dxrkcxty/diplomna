package repository;

import model.Order;
import model.User;
import model.Product;
import util.DatabaseConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class OrderRepository {
    private final UserRepository userRepository;

    private static final String ORDER_SELECT =
            "SELECT id, user_id, date, status, full_name, phone, delivery_address, delivery_carrier, delivery_branch, "
                    + "payment_method, payment_status, total_amount, bonus_used, bonus_earned, bonus_rate, items_snapshot FROM orders ";

    public OrderRepository() {
        this.userRepository = new UserRepository();
    }

    public List<Order> findAll() {
        List<Order> orders = queryOrders(ORDER_SELECT + "ORDER BY id", null);
        attachRelations(orders);
        return orders;
    }

    public Order findById(long id) {
        List<Order> orders = queryOrders(ORDER_SELECT + "WHERE id = ?", id);
        if (orders.isEmpty()) {
            return null;
        }
        attachRelations(orders);
        return orders.get(0);
    }

    public List<Order> findByUserId(long userId) {
        List<Order> orders = queryOrders(ORDER_SELECT + "WHERE user_id = ? ORDER BY id", userId);
        attachRelations(orders);
        return orders;
    }

    private List<Order> queryOrders(String sql, Long singleId) {
        List<Order> orders = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            if (singleId != null) {
                pstmt.setLong(1, singleId);
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    orders.add(mapResultSetToOrderShell(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Помилка отримання замовлень: " + e.getMessage());
        }
        return orders;
    }

    private void attachRelations(List<Order> orders) {
        if (orders.isEmpty()) {
            return;
        }
        Set<Long> userIds = orders.stream()
                .map(o -> o.getUser().getId())
                .collect(Collectors.toSet());
        List<Long> orderIds = orders.stream().map(Order::getId).toList();
        Map<Long, User> usersById = userRepository.findByIds(userIds);
        Map<Long, List<Product>> productsByOrderId = loadProductStubsByOrderIds(orderIds);
        for (Order order : orders) {
            User user = usersById.get(order.getUser().getId());
            if (user != null) {
                order.setUser(user);
            }
            order.setProducts(productsByOrderId.getOrDefault(order.getId(), new ArrayList<>()));
        }
    }

    private Map<Long, List<Product>> loadProductStubsByOrderIds(List<Long> orderIds) {
        Map<Long, List<Product>> result = new HashMap<>();
        for (Long orderId : orderIds) {
            result.put(orderId, new ArrayList<>());
        }
        if (orderIds.isEmpty()) {
            return result;
        }
        String placeholders = String.join(",", orderIds.stream().map(id -> "?").toList());
        String sqlWithQty = "SELECT order_id, product_id, quantity FROM order_products WHERE order_id IN (" + placeholders + ")";
        String sqlWithoutQty = "SELECT order_id, product_id FROM order_products WHERE order_id IN (" + placeholders + ")";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sqlWithQty)) {
            for (int i = 0; i < orderIds.size(); i++) {
                pstmt.setLong(i + 1, orderIds.get(i));
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    long orderId = rs.getLong("order_id");
                    long productId = rs.getLong("product_id");
                    int qty = Math.max(1, rs.getInt("quantity"));
                    List<Product> products = result.computeIfAbsent(orderId, k -> new ArrayList<>());
                    Product stub = new Product();
                    stub.setId(productId);
                    for (int i = 0; i < qty; i++) {
                        products.add(stub);
                    }
                }
            }
        } catch (SQLException e) {
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sqlWithoutQty)) {
                for (int i = 0; i < orderIds.size(); i++) {
                    pstmt.setLong(i + 1, orderIds.get(i));
                }
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        long orderId = rs.getLong("order_id");
                        long productId = rs.getLong("product_id");
                        List<Product> products = result.computeIfAbsent(orderId, k -> new ArrayList<>());
                        Product stub = new Product();
                        stub.setId(productId);
                        products.add(stub);
                    }
                }
            } catch (SQLException ex) {
                System.err.println("Помилка batch-отримання order_products: " + ex.getMessage());
            }
        }
        return result;
    }

    public Order save(Order order) {
        if (order.getId() == 0) {
            return insert(order);
        } else {
            return update(order);
        }
    }

    public void updatePaymentStatus(long orderId, String paymentStatus) {
        String sql = "UPDATE orders SET payment_status = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, paymentStatus);
            pstmt.setLong(2, orderId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Помилка оновлення статусу оплати: " + e.getMessage());
            throw new RuntimeException("Не вдалося оновити статус оплати", e);
        }
    }

    private Order insert(Order order) {
        String sql = "INSERT INTO orders (user_id, date, status, full_name, phone, delivery_address, delivery_carrier, delivery_branch, payment_method, payment_status, total_amount, bonus_used, bonus_earned, bonus_rate, items_snapshot) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, order.getUser().getId());
            pstmt.setString(2, order.getDate());
            pstmt.setString(3, order.getStatus());
            pstmt.setString(4, order.getFullName());
            pstmt.setString(5, order.getPhone());
            pstmt.setString(6, order.getDeliveryAddress());
            pstmt.setString(7, order.getDeliveryCarrier());
            pstmt.setString(8, order.getDeliveryBranch());
            pstmt.setString(9, order.getPaymentMethod());
            pstmt.setString(10, order.getPaymentStatus());
            pstmt.setDouble(11, order.getTotalAmount());
            pstmt.setDouble(12, order.getBonusUsed());
            pstmt.setDouble(13, order.getBonusEarned());
            if (order.getBonusRate() == null) pstmt.setNull(14, Types.INTEGER);
            else pstmt.setInt(14, order.getBonusRate());
            pstmt.setString(15, order.getItemsSnapshot());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    order.setId(rs.getLong("id"));
                    saveOrderProducts(order);
                    return order;
                }
            }
        } catch (SQLException e) {
            System.err.println("Помилка створення замовлення: " + e.getMessage());
            throw new RuntimeException("Не вдалося створити замовлення", e);
        }
        return null;
    }

    private Order update(Order order) {
        String sql = "UPDATE orders SET user_id = ?, date = ?, status = ?, full_name = ?, phone = ?, delivery_address = ?, delivery_carrier = ?, delivery_branch = ?, payment_method = ?, payment_status = ?, total_amount = ?, bonus_used = ?, bonus_earned = ?, bonus_rate = ?, items_snapshot = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, order.getUser().getId());
            pstmt.setString(2, order.getDate());
            pstmt.setString(3, order.getStatus());
            pstmt.setString(4, order.getFullName());
            pstmt.setString(5, order.getPhone());
            pstmt.setString(6, order.getDeliveryAddress());
            pstmt.setString(7, order.getDeliveryCarrier());
            pstmt.setString(8, order.getDeliveryBranch());
            pstmt.setString(9, order.getPaymentMethod());
            pstmt.setString(10, order.getPaymentStatus());
            pstmt.setDouble(11, order.getTotalAmount());
            pstmt.setDouble(12, order.getBonusUsed());
            pstmt.setDouble(13, order.getBonusEarned());
            if (order.getBonusRate() == null) pstmt.setNull(14, Types.INTEGER);
            else pstmt.setInt(14, order.getBonusRate());
            pstmt.setString(15, order.getItemsSnapshot());
            pstmt.setLong(16, order.getId());
            int rows = pstmt.executeUpdate();
            if (rows > 0) {
                deleteOrderProducts(order.getId());
                saveOrderProducts(order);
                return order;
            }
        } catch (SQLException e) {
            System.err.println("Помилка оновлення замовлення: " + e.getMessage());
            throw new RuntimeException("Не вдалося оновити замовлення", e);
        }
        return null;
    }

    private void saveOrderProducts(Order order) {
        if (order.getProducts() == null || order.getProducts().isEmpty()) {
            return;
        }
        String sqlWithQty = "INSERT INTO order_products (order_id, product_id, quantity) VALUES (?, ?, ?)";
        String sqlWithoutQty = "INSERT INTO order_products (order_id, product_id) VALUES (?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sqlWithQty)) {
            Map<Long, Integer> qtyByProductId = new HashMap<>();
            for (Product product : order.getProducts()) {
                long pid = product.getId();
                qtyByProductId.put(pid, qtyByProductId.getOrDefault(pid, 0) + 1);
            }

            for (Map.Entry<Long, Integer> e : qtyByProductId.entrySet()) {
                pstmt.setLong(1, order.getId());
                pstmt.setLong(2, e.getKey());
                pstmt.setInt(3, e.getValue());
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        } catch (SQLException e) {
            System.err.println("Помилка збереження order_products з quantity: " + e.getMessage());
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sqlWithoutQty)) {
                Set<Long> seen = new HashSet<>();
                for (Product product : order.getProducts()) {
                    long pid = product.getId();
                    if (seen.add(pid)) {
                        pstmt.setLong(1, order.getId());
                        pstmt.setLong(2, pid);
                        pstmt.addBatch();
                    }
                }
                pstmt.executeBatch();
            } catch (SQLException ex) {
                System.err.println("Помилка fallback збереження order_products: " + ex.getMessage());
                throw new RuntimeException("Не вдалося зберегти товари замовлення", ex);
            }
        }
    }

    private void deleteOrderProducts(long orderId) {
        String sql = "DELETE FROM order_products WHERE order_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, orderId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Помилка видалення товарів замовлення: " + e.getMessage());
        }
    }

    public List<Long> findOrderedProductIdsByUserId(long userId) {
        List<Long> productIds = new ArrayList<>();
        String sql = "SELECT DISTINCT op.product_id FROM order_products op "
                + "INNER JOIN orders o ON op.order_id = o.id WHERE o.user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    productIds.add(rs.getLong("product_id"));
                }
            }
        } catch (SQLException e) {
            System.err.println("Помилка отримання ID товарів: " + e.getMessage());
        }
        return productIds;
    }

    private Order mapResultSetToOrderShell(ResultSet rs) throws SQLException {
        Order order = new Order();
        order.setId(rs.getLong("id"));
        User user = new User();
        user.setId(rs.getLong("user_id"));
        order.setUser(user);
        order.setDate(rs.getString("date"));
        order.setStatus(rs.getString("status"));
        order.setFullName(rs.getString("full_name"));
        order.setPhone(rs.getString("phone"));
        order.setDeliveryAddress(rs.getString("delivery_address"));
        order.setDeliveryCarrier(rs.getString("delivery_carrier"));
        order.setDeliveryBranch(rs.getString("delivery_branch"));
        order.setPaymentMethod(rs.getString("payment_method"));
        order.setPaymentStatus(rs.getString("payment_status"));
        order.setTotalAmount(rs.getDouble("total_amount"));
        order.setBonusUsed(rs.getDouble("bonus_used"));
        order.setBonusEarned(rs.getDouble("bonus_earned"));
        int rate = rs.getInt("bonus_rate");
        order.setBonusRate(rs.wasNull() ? null : rate);
        try {
            order.setItemsSnapshot(rs.getString("items_snapshot"));
        } catch (SQLException ignored) {
            order.setItemsSnapshot(null);
        }
        order.setProducts(new ArrayList<>());
        return order;
    }
}
