package util;

import model.Order;
import model.User;
import model.Product;
import service.UserService;
import service.ProductService;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class OrderStorage {
    private static final String STORAGE_FILE = "orders.json";

    public static class OrderData {
        public long nextId;
        public List<Order> orders;

        public OrderData(long nextId, List<Order> orders) {
            this.nextId = nextId;
            this.orders = orders;
        }
    }

    public static void saveOrders(List<Order> orders, long nextId) {
        try {
            StringBuilder json = new StringBuilder();
            json.append("{\n");
            json.append("  \"nextId\": ").append(nextId).append(",\n");
            json.append("  \"orders\": [\n");
            
            for (int i = 0; i < orders.size(); i++) {
                Order o = orders.get(i);
                json.append("    {\n");
                json.append("      \"id\": ").append(o.getId()).append(",\n");
                json.append("      \"userId\": ").append(o.getUser() != null ? o.getUser().getId() : 0).append(",\n");
                json.append("      \"date\": \"").append(escapeJson(o.getDate())).append("\",\n");
                json.append("      \"status\": \"").append(escapeJson(o.getStatus())).append("\",\n");
                json.append("      \"productIds\": [");
                if (o.getProducts() != null && !o.getProducts().isEmpty()) {
                    for (int j = 0; j < o.getProducts().size(); j++) {
                        if (j > 0) json.append(", ");
                        json.append(o.getProducts().get(j).getId());
                    }
                }
                json.append("]\n");
                json.append("    }");
                if (i < orders.size() - 1) json.append(",");
                json.append("\n");
            }
            
            json.append("  ]\n");
            json.append("}\n");
            
            Files.write(Paths.get(STORAGE_FILE), json.toString().getBytes("UTF-8"));
        } catch (IOException e) {
            System.err.println("Помилка збереження замовлень: " + e.getMessage());
        }
    }

    public static OrderData loadOrders(UserService userService, ProductService productService) {
        try {
            File file = new File(STORAGE_FILE);
            if (!file.exists() || file.length() == 0) {
                return new OrderData(1, new ArrayList<>());
            }
            
            String content = new String(Files.readAllBytes(Paths.get(STORAGE_FILE)), "UTF-8");
            return parseJson(content, userService, productService);
        } catch (IOException e) {
            System.err.println("Помилка завантаження замовлень: " + e.getMessage());
            return new OrderData(1, new ArrayList<>());
        }
    }

    private static OrderData parseJson(String json, UserService userService, ProductService productService) {
        long nextId = 1;
        List<Order> orders = new ArrayList<>();
        
        try {
            int nextIdStart = json.indexOf("\"nextId\":");
            if (nextIdStart != -1) {
                int nextIdEnd = json.indexOf(",", nextIdStart);
                if (nextIdEnd == -1) nextIdEnd = json.indexOf("}", nextIdStart);
                String nextIdStr = json.substring(nextIdStart + 9, nextIdEnd).trim();
                nextId = Long.parseLong(nextIdStr);
            }
            
            int ordersStart = json.indexOf("\"orders\":[");
            if (ordersStart == -1) {
                return new OrderData(nextId, orders);
            }
            
            int pos = ordersStart + 10;
            while (pos < json.length()) {
                int objStart = json.indexOf("{", pos);
                if (objStart == -1) break;
                
                int objEnd = findMatchingBrace(json, objStart);
                if (objEnd == -1) break;
                
                String orderJson = json.substring(objStart, objEnd + 1);
                Order order = parseOrder(orderJson, userService, productService);
                if (order != null) {
                    orders.add(order);
                }
                
                pos = objEnd + 1;
                if (pos < json.length() && json.charAt(pos) == ']') break;
            }
        } catch (Exception e) {
            System.err.println("Помилка парсингу JSON замовлень: " + e.getMessage());
            e.printStackTrace();
        }
        
        return new OrderData(nextId, orders);
    }

    private static int findMatchingBrace(String json, int start) {
        int depth = 0;
        for (int i = start; i < json.length(); i++) {
            if (json.charAt(i) == '{') depth++;
            if (json.charAt(i) == '}') {
                depth--;
                if (depth == 0) return i;
            }
        }
        return -1;
    }

    private static Order parseOrder(String json, UserService userService, ProductService productService) {
        try {
            long id = extractLong(json, "id");
            long userId = extractLong(json, "userId");
            String date = extractString(json, "date");
            String status = extractString(json, "status");
            
            List<Long> productIds = new ArrayList<>();
            int productIdsStart = json.indexOf("\"productIds\":[");
            if (productIdsStart != -1) {
                int arrayStart = productIdsStart + 14;
                int arrayEnd = json.indexOf("]", arrayStart);
                if (arrayEnd != -1) {
                    String idsStr = json.substring(arrayStart, arrayEnd).trim();
                    if (!idsStr.isEmpty()) {
                        String[] ids = idsStr.split(",");
                        for (String idStr : ids) {
                            try {
                                productIds.add(Long.parseLong(idStr.trim()));
                            } catch (NumberFormatException e) {
                            }
                        }
                    }
                }
            }
            
            Order order = new Order();
            order.setId(id);
            order.setUser(userService.getById(userId));
            order.setDate(date);
            order.setStatus(status);
            
            List<Product> products = new ArrayList<>();
            for (Long productId : productIds) {
                Product product = productService.getById(productId);
                if (product != null) {
                    products.add(product);
                }
            }
            order.setProducts(products);
            
            return order;
        } catch (Exception e) {
            System.err.println("Помилка парсингу замовлення: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private static long extractLong(String json, String key) {
        String pattern = "\"" + key + "\":";
        int start = json.indexOf(pattern);
        if (start == -1) return 0;
        start += pattern.length();
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) {
            end++;
        }
        try {
            return Long.parseLong(json.substring(start, end).trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String extractString(String json, String key) {
        String pattern = "\"" + key + "\":\"";
        int start = json.indexOf(pattern);
        if (start == -1) return "";
        start += pattern.length();
        int end = start;
        while (end < json.length() && json.charAt(end) != '"') {
            if (json.charAt(end) == '\\' && end + 1 < json.length()) {
                end += 2;
            } else {
                end++;
            }
        }
        String value = json.substring(start, end);
        return unescapeJson(value);
    }

    private static String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }

    private static String unescapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\\"", "\"")
                   .replace("\\n", "\n")
                   .replace("\\r", "\r")
                   .replace("\\t", "\t")
                   .replace("\\\\", "\\");
    }
}

