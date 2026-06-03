package controller;

import com.sun.net.httpserver.*;
import service.OrderService;
import service.UserService;
import model.Order;
import model.User;
import model.OrderMessage;
import dto.OrderDTO;
import util.JwtUtil;
import repository.OrderMessageRepository;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class OrderController implements HttpHandler {
    private final OrderService orderService;
    private final UserService userService;
    private final OrderMessageRepository messageRepository;

    public OrderController(OrderService orderService, UserService userService) {
        this.orderService = orderService;
        this.userService = userService;
        this.messageRepository = new OrderMessageRepository();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            setCorsHeaders(exchange);
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
            return;
        }

        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        String[] pathParts = path.split("/");

        String response = "";
        int status = 200;

        try {
            if (method.equals("POST") && pathParts.length == 3 && "api".equals(pathParts[1]) && "orders".equals(pathParts[2])) {
                if (!checkAuth(exchange, false)) return;
                
                Long userId = getUserIdFromToken(exchange);
                if (userId == null) {
                    status = 401;
                    response = toError(401, "Unauthorized - користувач не знайдено");
                } else {
                    try {
                        OrderDTO dto = readDtoFromBody(exchange.getRequestBody());
                        if (dto.productIds().isEmpty()) {
                            status = 400;
                            response = toError(400, "Корзина порожня");
                        } else {
                            Order order = orderService.createOrder(
                                    userId,
                                    dto.productIds(),
                                    dto.itemsSnapshot(),
                                    dto.fullName(),
                                    dto.phone(),
                                    dto.deliveryAddress(),
                                    dto.deliveryCarrier(),
                                    dto.deliveryBranch(),
                                    dto.paymentMethod(),
                                    dto.paymentStatus(),
                                    dto.bonusUsed()
                            );
                            response = toJson(toDTO(order));
                            status = 201;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        status = 400;
                        response = toError(400, e.getMessage());
                    }
                }
            } else if (method.equals("GET") && pathParts.length == 3 && "api".equals(pathParts[1]) && "orders".equals(pathParts[2])) {
                if (!checkAuth(exchange, false)) return;

                User currentUser = getCurrentUserFromToken(exchange);
                if (currentUser == null) {
                    status = 401;
                    response = toError(401, "Unauthorized");
                    sendResponse(exchange, status, response);
                    return;
                }
                List<OrderDTO> orders = "ADMIN".equalsIgnoreCase(currentUser.getRole())
                        ? orderService.getAllOrders().stream().map(this::toDTO).collect(Collectors.toList())
                        : orderService.getOrdersByUser(currentUser.getId()).stream().map(this::toDTO).collect(Collectors.toList());
                response = toJsonArray(orders);
            } else if (method.equals("POST")
                    && (
                        (pathParts.length == 5 && "api".equals(pathParts[1]) && "orders".equals(pathParts[2]) && "spin-bonus".equals(pathParts[4]))
                        || (pathParts.length == 6 && "api".equals(pathParts[1]) && "orders".equals(pathParts[2]) && "spin-bonus".equals(pathParts[4]) && (pathParts[5] == null || pathParts[5].isBlank()))
                    )) {
                if (!checkAuth(exchange, false)) return;
                User currentUser = getCurrentUserFromToken(exchange);
                if (currentUser == null) {
                    status = 401;
                    response = toError(401, "Unauthorized");
                    sendResponse(exchange, status, response);
                    return;
                }
                if ("ADMIN".equalsIgnoreCase(currentUser.getRole())) {
                    status = 403;
                    response = toError(403, "Адміністратор не може крутити бонуси");
                    sendResponse(exchange, status, response);
                    return;
                }
                long orderId = Long.parseLong(pathParts[3]);
                try {
                    Order updated = orderService.spinBonusForOrder(currentUser.getId(), orderId);
                    response = toJson(toDTO(updated));
                    status = 200;
                } catch (IllegalArgumentException ex) {
                    status = 400;
                    response = toError(400, ex.getMessage());
                }
            } else if (pathParts.length == 5 && "api".equals(pathParts[1]) && "orders".equals(pathParts[2]) && "messages".equals(pathParts[4])) {
                if (!checkAuth(exchange, false)) return;
                long orderId = Long.parseLong(pathParts[3]);
                if (!canAccessOrder(exchange, orderId)) {
                    status = 403;
                    response = toError(403, "Forbidden");
                    sendResponse(exchange, status, response);
                    return;
                }
                if (method.equals("GET")) {
                    List<OrderMessage> messages = messageRepository.findByOrderId(orderId);
                    response = toJsonMessages(messages);
                } else if (method.equals("POST")) {
                    String body = readRequestBody(exchange.getRequestBody());
                    String messageText = extractJsonString(body, "message");
                    if (messageText == null || messageText.trim().isEmpty()) {
                        status = 400;
                        response = toError(400, "Повідомлення порожнє");
                        sendResponse(exchange, status, response);
                        return;
                    }
                    String[] tokenData = getTokenData(exchange);
                    if (tokenData == null || tokenData.length < 2) {
                        status = 401;
                        response = toError(401, "Unauthorized");
                        sendResponse(exchange, status, response);
                        return;
                    }
                    OrderMessage msg = new OrderMessage();
                    msg.setOrderId(orderId);
                    msg.setSenderEmail(tokenData[0]);
                    msg.setSenderRole(tokenData[1]);
                    msg.setMessage(messageText.trim());
                    msg.setCreatedAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                    OrderMessage created = messageRepository.insert(msg);
                    response = toJsonMessage(created);
                    status = 201;
                } else {
                    status = 405;
                    response = toError(405, "Method Not Allowed");
                }
            } else if (method.equals("POST") && pathParts.length == 5
                    && "api".equals(pathParts[1]) && "orders".equals(pathParts[2])
                    && "confirm-payment".equals(pathParts[4])) {
                if (!checkAuth(exchange, true)) {
                    return;
                }
                try {
                    long orderId = Long.parseLong(pathParts[3]);
                    Order order = orderService.confirmPayment(orderId);
                    response = toJson(toDTO(order));
                    status = 200;
                } catch (NumberFormatException e) {
                    status = 400;
                    response = toError(400, "Невірний ID замовлення");
                } catch (IllegalArgumentException e) {
                    status = 404;
                    response = toError(404, e.getMessage());
                } catch (Exception e) {
                    e.printStackTrace();
                    status = 500;
                    response = toError(500, e.getMessage());
                }
            } else if (method.equals("PUT") && pathParts.length == 4 && "api".equals(pathParts[1]) && "orders".equals(pathParts[2])) {
                if (!checkAuth(exchange, true)) {
                    return;
                }
                
                try {
                    long orderId = Long.parseLong(pathParts[3]);
                    String requestBody = readRequestBody(exchange.getRequestBody());
                    String newStatus = extractJsonStringOrNull(requestBody, "status");
                    String newPaymentStatus = extractJsonStringOrNull(requestBody, "paymentStatus");

                    Order order = orderService.updateOrder(orderId, newStatus, newPaymentStatus);
                    response = toJson(toDTO(order));
                    status = 200;
                } catch (NumberFormatException e) {
                    status = 400;
                    response = toError(400, "Невірний ID замовлення");
                } catch (IllegalArgumentException e) {
                    status = 404;
                    response = toError(404, e.getMessage());
                } catch (Exception e) {
                    e.printStackTrace();
                    status = 500;
                    response = toError(500, e.getMessage());
                }
            } else {
                status = 404;
                response = toError(404, "Not found");
            }
        } catch (Exception e) {
            status = 500;
            response = toError(500, e.getMessage());
        }

        sendResponse(exchange, status, response);
    }

    private Long getUserIdFromToken(HttpExchange exchange) {
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                String[] tokenData = JwtUtil.validateToken(token);
                if (tokenData != null && tokenData.length >= 2) {
                    String email = tokenData[0];
                    User user = userService.findByEmail(email);
                    if (user != null) {
                        return user.getId();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
        return null;
    }

    private boolean checkAuth(HttpExchange exchange, boolean needAdmin) {
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            try {
                sendResponse(exchange, 401, toError(401, "Unauthorized"));
            } catch (IOException e) {
            }
            return false;
        }

        String token = authHeader.substring(7);
        try {
            String[] tokenData = JwtUtil.validateToken(token);
            if (tokenData == null || tokenData.length < 2) {
                try {
                    sendResponse(exchange, 401, toError(401, "Unauthorized"));
                } catch (IOException e) {
                }
                return false;
            }
            if (needAdmin && !"ADMIN".equalsIgnoreCase(tokenData[1])) {
                try {
                    sendResponse(exchange, 403, toError(403, "Forbidden"));
                } catch (IOException e) {
                }
                return false;
            }
            return true;
        } catch (Exception e) {
            try {
                sendResponse(exchange, 401, toError(401, "Unauthorized"));
            } catch (IOException ioException) {
            }
            return false;
        }
    }

    private String[] getTokenData(HttpExchange exchange) {
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        String token = authHeader.substring(7);
        return JwtUtil.validateToken(token);
    }

    private boolean isAdmin(HttpExchange exchange) {
        String[] data = getTokenData(exchange);
        return data != null && data.length >= 2 && "ADMIN".equalsIgnoreCase(data[1]);
    }

    private boolean canAccessOrder(HttpExchange exchange, long orderId) {
        User currentUser = getCurrentUserFromToken(exchange);
        if (currentUser == null) return false;
        if ("ADMIN".equalsIgnoreCase(currentUser.getRole())) return true;
        Order order = orderService.getById(orderId);
        return order != null && order.getUser() != null && order.getUser().getId() == currentUser.getId();
    }

    private User getCurrentUserFromToken(HttpExchange exchange) {
        String[] data = getTokenData(exchange);
        if (data == null || data.length < 1) return null;
        return userService.findByEmail(data[0]);
    }

    private OrderDTO readDtoFromBody(InputStream is) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        StringBuilder body = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            body.append(line);
        }

        String json = body.toString().trim();
        
        if (!json.startsWith("{") || !json.endsWith("}")) {
            throw new IOException("Invalid JSON format");
        }

        List<Long> productIds = parseProductIds(json);
        String fullName = extractJsonString(json, "fullName");
        String phone = extractJsonString(json, "phone");
        String deliveryAddress = extractJsonString(json, "deliveryAddress");
        String deliveryCarrier = extractJsonString(json, "deliveryCarrier");
        String deliveryBranch = extractJsonString(json, "deliveryBranch");
        String paymentMethod = extractJsonString(json, "paymentMethod");
        String paymentStatus = extractJsonString(json, "paymentStatus");
        double bonusUsed = extractJsonDouble(json, "bonusUsed", 0.0);

        String itemsSnapshot = extractJsonArrayRaw(json, "items");
        return new OrderDTO(0, 0, productIds, null, null, fullName, phone, deliveryAddress, deliveryCarrier, deliveryBranch, paymentMethod, paymentStatus, 0.0, bonusUsed, 0.0, null, itemsSnapshot);
    }

    private String readRequestBody(InputStream is) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        StringBuilder body = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            body.append(line);
        }
        return body.toString().trim();
    }

    private String extractJsonStringOrNull(String json, String key) {
        if (json == null || json.isBlank()) {
            return null;
        }
        String pattern = "\"" + key + "\"";
        int keyIndex = indexOfJsonKey(json, pattern);
        if (keyIndex == -1) {
            return null;
        }
        String value = extractJsonStringAt(json, keyIndex, pattern);
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private int indexOfJsonKey(String json, String pattern) {
        int from = 0;
        while (from < json.length()) {
            int idx = json.indexOf(pattern, from);
            if (idx == -1) {
                return -1;
            }
            if (idx > 0) {
                char before = json.charAt(idx - 1);
                if (Character.isLetterOrDigit(before) || before == '_') {
                    from = idx + 1;
                    continue;
                }
            }
            return idx;
        }
        return -1;
    }

    private String extractJsonStringAt(String json, int keyIndex, String pattern) {
        int colonIndex = json.indexOf(":", keyIndex + pattern.length());
        if (colonIndex == -1) {
            return "";
        }
        int firstQuote = json.indexOf("\"", colonIndex + 1);
        if (firstQuote == -1) {
            return "";
        }
        int current = firstQuote + 1;
        StringBuilder value = new StringBuilder();
        while (current < json.length()) {
            char ch = json.charAt(current);
            if (ch == '"' && json.charAt(current - 1) != '\\') {
                break;
            }
            value.append(ch);
            current++;
        }
        return value.toString()
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }

    private OrderDTO toDTO(Order order) {
        List<Long> productIds = order.getProducts().stream()
                .map(p -> p.getId())
                .collect(Collectors.toList());
        return new OrderDTO(
                order.getId(),
                order.getUser().getId(),
                productIds,
                order.getDate(),
                order.getStatus(),
                order.getFullName(),
                order.getPhone(),
                order.getDeliveryAddress(),
                order.getDeliveryCarrier(),
                order.getDeliveryBranch(),
                order.getPaymentMethod(),
                order.getPaymentStatus(),
                order.getTotalAmount(),
                order.getBonusUsed(),
                order.getBonusEarned(),
                order.getBonusRate(),
                order.getItemsSnapshot()
        );
    }

    private String toJson(OrderDTO order) {
        String productIdsStr = order.productIds().stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
        String itemsJson = (order.itemsSnapshot() != null && !order.itemsSnapshot().isBlank())
                ? order.itemsSnapshot().trim()
                : "[]";
        if (!itemsJson.startsWith("[")) {
            itemsJson = "[]";
        }
        return String.format(
                "{\"id\":%d,\"userId\":%d,\"productIds\":[%s],\"items\":%s,\"date\":\"%s\",\"status\":\"%s\",\"fullName\":\"%s\",\"phone\":\"%s\",\"deliveryAddress\":\"%s\",\"deliveryCarrier\":\"%s\",\"deliveryBranch\":\"%s\",\"paymentMethod\":\"%s\",\"paymentStatus\":\"%s\",\"totalAmount\":%s,\"bonusUsed\":%s,\"bonusEarned\":%s,\"bonusRate\":%s}",
                order.id(),
                order.userId(),
                productIdsStr,
                itemsJson,
                escapeJson(order.date() != null ? order.date() : ""),
                escapeJson(order.status() != null ? order.status() : ""),
                escapeJson(order.fullName() != null ? order.fullName() : ""),
                escapeJson(order.phone() != null ? order.phone() : ""),
                escapeJson(order.deliveryAddress() != null ? order.deliveryAddress() : ""),
                escapeJson(order.deliveryCarrier() != null ? order.deliveryCarrier() : ""),
                escapeJson(order.deliveryBranch() != null ? order.deliveryBranch() : ""),
                escapeJson(order.paymentMethod() != null ? order.paymentMethod() : ""),
                escapeJson(order.paymentStatus() != null ? order.paymentStatus() : ""),
                jsonNumber(order.totalAmount()),
                jsonNumber(order.bonusUsed()),
                jsonNumber(order.bonusEarned()),
                order.bonusRate() == null ? "null" : String.valueOf(order.bonusRate())
        );
    }

    private double extractJsonDouble(String json, String key, double defaultValue) {
        try {
            String pattern = "\"" + key + "\"";
            int idx = json.indexOf(pattern);
            if (idx == -1) return defaultValue;
            int colon = json.indexOf(":", idx + pattern.length());
            if (colon == -1) return defaultValue;
            int start = colon + 1;
            while (start < json.length() && Character.isWhitespace(json.charAt(start))) start++;
            int end = start;
            while (end < json.length()) {
                char c = json.charAt(end);
                if ((c >= '0' && c <= '9') || c == '.' || c == '-') {
                    end++;
                } else {
                    break;
                }
            }
            if (end <= start) return defaultValue;
            return Double.parseDouble(json.substring(start, end));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private String toJsonArray(List<OrderDTO> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); ++i) {
            if (i > 0) sb.append(",");
            sb.append(toJson(list.get(i)));
        }
        sb.append("]");
        return sb.toString();
    }

    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }

    private String toError(int status, String message) {
        return String.format("{\"status\":%d,\"error\":\"%s\",\"message\":\"%s\"}", 
                status, escapeJson("Error"), escapeJson(message));
    }

    private String jsonNumber(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) return "0";
        return java.math.BigDecimal.valueOf(value)
                .setScale(2, java.math.RoundingMode.HALF_UP)
                .toPlainString();
    }

    private List<Long> parseProductIds(String json) {
        List<Long> productIds = new ArrayList<>();
        if (!json.contains("\"productIds\"")) {
            return productIds;
        }
        int start = json.indexOf("\"productIds\"") + 12;
        int arrayStart = json.indexOf("[", start);
        int arrayEnd = json.indexOf("]", arrayStart);
        if (arrayStart == -1 || arrayEnd == -1 || arrayEnd <= arrayStart) {
            return productIds;
        }
        String idsStr = json.substring(arrayStart + 1, arrayEnd);
        if (idsStr.trim().isEmpty()) {
            return productIds;
        }
        for (String id : idsStr.split(",")) {
            try {
                productIds.add(Long.parseLong(id.trim()));
            } catch (NumberFormatException ignored) {
            }
        }
        return productIds;
    }

    private String extractJsonArrayRaw(String json, String key) {
        String pattern = "\"" + key + "\"";
        int keyIndex = json.indexOf(pattern);
        if (keyIndex == -1) return "";
        int arrayStart = json.indexOf("[", keyIndex + pattern.length());
        if (arrayStart == -1) return "";
        int depth = 0;
        for (int i = arrayStart; i < json.length(); i++) {
            char ch = json.charAt(i);
            if (ch == '[') depth++;
            else if (ch == ']') {
                depth--;
                if (depth == 0) {
                    return json.substring(arrayStart, i + 1);
                }
            }
        }
        return "";
    }

    private String extractJsonString(String json, String key) {
        String pattern = "\"" + key + "\"";
        int keyIndex = indexOfJsonKey(json, pattern);
        if (keyIndex == -1) return "";
        int colonIndex = json.indexOf(":", keyIndex + pattern.length());
        if (colonIndex == -1) return "";
        int firstQuote = json.indexOf("\"", colonIndex + 1);
        if (firstQuote == -1) return "";
        int current = firstQuote + 1;
        StringBuilder value = new StringBuilder();
        while (current < json.length()) {
            char ch = json.charAt(current);
            if (ch == '"' && json.charAt(current - 1) != '\\') {
                break;
            }
            value.append(ch);
            current++;
        }
        return value.toString()
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }

    private void setCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }

    private void sendResponse(HttpExchange exchange, int status, String response) throws IOException {
        setCorsHeaders(exchange);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, responseBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }

    private String toJsonMessage(OrderMessage m) {
        if (m == null) return "{}";
        return String.format(
                "{\"id\":%d,\"orderId\":%d,\"senderEmail\":\"%s\",\"senderRole\":\"%s\",\"message\":\"%s\",\"createdAt\":\"%s\"}",
                m.getId(),
                m.getOrderId(),
                escapeJson(m.getSenderEmail()),
                escapeJson(m.getSenderRole()),
                escapeJson(m.getMessage()),
                escapeJson(m.getCreatedAt())
        );
    }

    private String toJsonMessages(List<OrderMessage> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); ++i) {
            if (i > 0) sb.append(",");
            sb.append(toJsonMessage(list.get(i)));
        }
        sb.append("]");
        return sb.toString();
    }
}

