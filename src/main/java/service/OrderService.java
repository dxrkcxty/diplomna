package service;

import model.Order;
import model.User;
import model.Product;
import repository.OrderRepository;
import java.util.*;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class OrderService {
    private final OrderRepository orderRepository;
    private final UserService userService;
    private final ProductService productService;

    public OrderService(UserService userService, ProductService productService) {
        this.userService = userService;
        this.productService = productService;
        this.orderRepository = new OrderRepository();
    }

    public Order createOrder(long userId, List<Long> productIds, String itemsSnapshot, String fullName, String phone, String deliveryAddress,
                             String deliveryCarrier, String deliveryBranch, String paymentMethod, String paymentStatus,
                             double bonusUsed) {
        User user = userService.getById(userId);
        if (user == null) {
            throw new IllegalArgumentException("Користувач не знайдено");
        }

        List<Product> products = new ArrayList<>();
        for (Long productId : productIds) {
            Product product = productService.getById(productId);
            if (product != null) {
                products.add(product);
            }
        }

        if (products.isEmpty()) {
            throw new IllegalArgumentException("Корзина порожня");
        }

        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("Вкажіть ПІБ");
        }
        if (phone == null || phone.isBlank()) {
            throw new IllegalArgumentException("Вкажіть номер телефону");
        }
        if (!phone.trim().matches("^\\+380\\d{9}$")) {
            throw new IllegalArgumentException("Номер телефону має бути у форматі +380XXXXXXXXX");
        }
        if (deliveryAddress == null || deliveryAddress.isBlank()) {
            throw new IllegalArgumentException("Вкажіть адресу доставки");
        }
        if (deliveryCarrier == null || deliveryCarrier.isBlank()) {
            throw new IllegalArgumentException("Оберіть поштову службу");
        }
        if (deliveryBranch == null || deliveryBranch.isBlank()) {
            throw new IllegalArgumentException("Оберіть відділення");
        }
        if (paymentMethod == null || paymentMethod.isBlank()) {
            throw new IllegalArgumentException("Оберіть спосіб оплати");
        }
        if (bonusUsed < 0) {
            throw new IllegalArgumentException("Некоректна сума бонусів");
        }

        Order order = new Order();
        order.setUser(user);
        order.setProducts(products);
        order.setDate(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        order.setStatus("Нове");
        order.setFullName(fullName.trim());
        order.setPhone(phone.trim());
        order.setDeliveryAddress(deliveryAddress.trim());
        order.setDeliveryCarrier(deliveryCarrier.trim());
        order.setDeliveryBranch(deliveryBranch.trim());
        order.setPaymentMethod(paymentMethod.trim());
        order.setPaymentStatus(paymentStatus == null || paymentStatus.isBlank() ? "Очікує оплату" : paymentStatus.trim());

        double total;
        if (itemsSnapshot != null && !itemsSnapshot.isBlank()) {
            total = sumPricesFromItemsSnapshot(itemsSnapshot);
            if (total <= 0) {
                total = products.stream().mapToDouble(Product::getFinalPrice).sum();
            }
        } else {
            total = products.stream().mapToDouble(Product::getFinalPrice).sum();
        }
        total = Math.round(total * 100.0) / 100.0;
        order.setTotalAmount(total);
        order.setItemsSnapshot(itemsSnapshot);
        double available = Math.max(0, user.getBonusBalance());
        double maxUse = Math.min(total, available);
        double use = Math.min(bonusUsed, maxUse);
        use = Math.round(use * 100.0) / 100.0;

        order.setBonusUsed(use);
        order.setBonusEarned(0.0);
        order.setBonusRate(null);

        Order saved = orderRepository.save(order);

        double newBalance = Math.round((available - use) * 100.0) / 100.0;
        user.setBonusBalance(newBalance);
        userService.update(user.getId(), new User(
                user.getId(),
                user.getEmail(),
                "",
                user.getRole(),
                user.getFirstName(),
                user.getLastName(),
                user.getBonusBalance(),
                user.getBonusRate(),
                user.getSpinCredits(),
                List.of()
        ));
        return saved;
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public List<Order> getOrdersByUser(long userId) {
        return orderRepository.findByUserId(userId);
    }

    public Order getById(long id) {
        return orderRepository.findById(id);
    }

    public Order updateOrderStatus(long id, String status) {
        return updateOrder(id, status, null);
    }

    public Order confirmPayment(long id) {
        Order order = orderRepository.findById(id);
        if (order == null) {
            throw new IllegalArgumentException("Замовлення не знайдено");
        }
        orderRepository.updatePaymentStatus(id, "Оплачено");
        order.setPaymentStatus("Оплачено");
        return order;
    }

    public Order updateOrder(long id, String status, String paymentStatus) {
        Order order = orderRepository.findById(id);
        if (order == null) {
            throw new IllegalArgumentException("Замовлення не знайдено");
        }
        boolean hasStatus = status != null && !status.isBlank();
        boolean hasPayment = paymentStatus != null && !paymentStatus.isBlank();
        if (!hasStatus && !hasPayment) {
            throw new IllegalArgumentException("Потрібно вказати status або paymentStatus");
        }
        if (hasStatus) {
            order.setStatus(status.trim());
        }
        if (hasPayment) {
            order.setPaymentStatus(paymentStatus.trim());
        }
        return orderRepository.save(order);
    }

    public Order spinBonusForOrder(long userId, long orderId) {
        Order order = orderRepository.findById(orderId);
        if (order == null) {
            throw new IllegalArgumentException("Замовлення не знайдено");
        }
        if (order.getUser() == null || order.getUser().getId() != userId) {
            throw new IllegalArgumentException("Немає доступу до замовлення");
        }
        if (!"Відправлено".equalsIgnoreCase(String.valueOf(order.getStatus()))) {
            throw new IllegalArgumentException("Крутити бонус можна тільки після підтвердження замовлення адміністратором");
        }
        if (order.getBonusRate() != null || order.getBonusEarned() > 0.0) {
            throw new IllegalArgumentException("Для цього замовлення бонус вже нараховано");
        }

        double total = Math.max(0, order.getTotalAmount());
        if (total <= 0) {
            List<Product> products = order.getProducts() == null ? List.of() : order.getProducts();
            total = products.stream().mapToDouble(Product::getFinalPrice).sum();
        }
        double bonusUsed = Math.max(0, order.getBonusUsed());
        double base = Math.max(0, total - bonusUsed);
        int rate = 2 + new java.util.Random().nextInt(4);
        double earned = Math.round(base * (rate / 100.0) * 100.0) / 100.0;

        order.setBonusRate(rate);
        order.setBonusEarned(earned);
        Order saved = orderRepository.save(order);

        User user = userService.getById(userId);
        if (user != null) {
            double newBalance = Math.round((Math.max(0, user.getBonusBalance()) + earned) * 100.0) / 100.0;
            user.setBonusBalance(newBalance);
            user.setBonusRate(rate);
            userService.update(user.getId(), new User(
                    user.getId(),
                    user.getEmail(),
                    "",
                    user.getRole(),
                    user.getFirstName(),
                    user.getLastName(),
                    user.getBonusBalance(),
                    user.getBonusRate(),
                    user.getSpinCredits(),
                    List.of()
            ));
        }
        return saved;
    }

    public boolean hasUserOrderedProduct(long userId, long productId) {
        List<Long> productIds = orderRepository.findOrderedProductIdsByUserId(userId);
        return productIds.contains(productId);
    }

    public List<Long> getOrderedProductIds(long userId) {
        return orderRepository.findOrderedProductIdsByUserId(userId);
    }

    private double sumPricesFromItemsSnapshot(String itemsSnapshot) {
        double sum = 0;
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("\"price\"\\s*:\\s*([0-9]+(?:\\.[0-9]+)?)")
                .matcher(itemsSnapshot);
        while (matcher.find()) {
            sum += Double.parseDouble(matcher.group(1));
        }
        return sum;
    }
}

