package controller;

import com.sun.net.httpserver.*;
import service.ProductService;
import service.CategoryService;
import model.Product;
import model.Category;
import model.Review;
import dto.ProductDTO;
import util.ValidationUtil;
import util.JwtUtil;
import exception.ErrorResponse;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;
import java.net.URI;
import java.net.URLDecoder;
import java.util.Map;
import java.util.HashMap;
import java.util.Locale;

public class ProductController implements HttpHandler {
    private final ProductService productService;
    private final CategoryService categoryService;

    public ProductController(ProductService productService) {
        this.productService = productService;
        this.categoryService = new CategoryService();
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
        
        if (path.contains(".") && (path.endsWith(".html") || path.endsWith(".css") || path.endsWith(".js") || path.endsWith(".json"))) {
            exchange.sendResponseHeaders(404, 0);
            exchange.close();
            return;
        }
        
        String[] pathParts = path.split("/");
        String response = "";
        int status = 200;
        try {
            if (method.equals("GET") && pathParts.length == 3 && "api".equals(pathParts[1]) && "products".equals(pathParts[2])) {
                Map<String, String> params = parseQueryParams(exchange.getRequestURI());
                String discountOnly = params.get("discountOnly");
                
                if ("true".equals(discountOnly)) {
                    List<Product> products = productService.getDiscountedProducts();
                    response = toJsonArrayFromProducts(products);
                } else {
                    String name = params.get("name");
                    Double minPrice = params.get("minPrice") != null ? Double.valueOf(params.get("minPrice")) : null;
                    Double maxPrice = params.get("maxPrice") != null ? Double.valueOf(params.get("maxPrice")) : null;
                    Long categoryId = params.get("categoryId") != null ? Long.valueOf(params.get("categoryId")) : null;
                    String sortBy = params.get("sortBy");
                    String sortOrder = params.get("sortOrder");
                    Integer offset = params.get("offset") != null ? Integer.valueOf(params.get("offset")) : null;
                    Integer limit = params.get("limit") != null ? Integer.valueOf(params.get("limit")) : null;
                    List<Product> products = productService.getFiltered(name, minPrice, maxPrice, categoryId, sortBy, sortOrder, offset, limit);
                    response = toJsonArrayFromProducts(products);
                }
            } else if (method.equals("GET") && pathParts.length == 4 && "api".equals(pathParts[1]) && "products".equals(pathParts[2])) {
                try {
                    long id = Long.parseLong(pathParts[3]);
                    Product p = productService.getById(id);
                    if (p == null) { status = 404; response = toError(404, "Not found"); }
                    else response = toJsonFromProduct(p);
                } catch (NumberFormatException e) {
                    status = 404;
                    response = toError(404, "Not found");
                }
            } else if (method.equals("POST") && pathParts.length == 3 && "api".equals(pathParts[1]) && "products".equals(pathParts[2])) {
                if (!checkAuth(exchange, true)) return;
                ProductDTO dto = readDtoFromBody(exchange.getRequestBody());
                try { 
                    ValidationUtil.validate(dto); 
                } catch (IllegalArgumentException e) {
                    status = 400; response = toError(400, e.getMessage()); sendResponse(exchange, status, response); return;
                }
                Product product = fromDTO(dto);
                Product created = productService.create(product);
                response = toJsonFromProduct(created);
                status = 201;
                setCorsHeaders(exchange);
                exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
                exchange.sendResponseHeaders(status, response.getBytes(StandardCharsets.UTF_8).length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes(StandardCharsets.UTF_8));
                }
                return;
            } else if (method.equals("PUT") && pathParts.length == 4 && "api".equals(pathParts[1]) && "products".equals(pathParts[2])) {
                if (!checkAuth(exchange, true)) return;
                long id = Long.parseLong(pathParts[3]);
                ProductDTO dto = readDtoFromBody(exchange.getRequestBody());
                try { ValidationUtil.validate(dto); } catch (IllegalArgumentException e) {
                    status = 400; response = toError(400, e.getMessage()); sendResponse(exchange, status, response); return;
                }
                Product product = fromDTO(dto);
                Product updated = productService.update(id, product);
                if (updated == null) { status = 404; response = toError(404, "Not found"); }
                else response = toJsonFromProduct(updated);
            } else if (method.equals("DELETE") && pathParts.length == 4 && "api".equals(pathParts[1]) && "products".equals(pathParts[2])) {
                if (!checkAuth(exchange, true)) return;
                long id = Long.parseLong(pathParts[3]);
                boolean removed = productService.delete(id);
                if (removed) { status = 204; response = ""; }
                else { status = 404; response = toError(404, "Not found"); }
            } else {
                status = 405; response = toError(405, "Method Not Allowed");
            }
        } catch(Exception ex) {
            status = 500; response = toError(500, ex.getMessage());
        }
        setCorsHeaders(exchange);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(status, response.getBytes(StandardCharsets.UTF_8).length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes(StandardCharsets.UTF_8));
        }
    }
    
    private void setCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }
    private ProductDTO toDTO(Product p) {
        long categoryId = (p.getCategory() != null) ? p.getCategory().getId() : 0L;
        return new ProductDTO(p.getId(), p.getName(), p.getPrice(), categoryId, 
                             p.getType(), p.getSize(), p.getGender(), p.getImageUrl(),
                             p.getDiscountPercent(), p.getDiscountAmount());
    }
    private Product fromDTO(ProductDTO dto) {
        Category category = null;
        if (dto.categoryId() > 0) {
            category = categoryService.getById(dto.categoryId());
        }
        if (category == null && dto.type() != null && !dto.type().isBlank()) {
            category = categoryService.resolveByProductType(dto.type());
        }
        Product product = new Product(dto.id(), dto.name(), dto.price(), category, null,
                dto.type(), dto.size(), dto.gender(), dto.imageUrl());
        product.setDiscountPercent(dto.discountPercent());
        product.setDiscountAmount(dto.discountAmount());
        return product;
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
    private Map<String, String> parseQueryParams(URI uri) {
        Map<String, String> map = new HashMap<>();
        String q = uri.getQuery();
        if (q != null) {
            for (String param : q.split("&")) {
                String[] parts = param.split("=", 2);
                if (parts.length == 2) map.put(parts[0], parts[1]);
            }
        }
        return map;
    }
    private boolean checkAuth(HttpExchange exchange, boolean needAdmin) throws IOException {
        String auth = exchange.getRequestHeaders().getFirst("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            sendResponse(exchange, 401, toError(401, "Auth required"));
            return false;
        }
        String token = auth.substring(7);
        String[] data = JwtUtil.validateToken(token);
        if (data == null) {
            sendResponse(exchange, 401, toError(401, "Invalid token"));
            return false;
        }
        if (needAdmin && !"ADMIN".equals(data[1])) {
            sendResponse(exchange, 403, toError(403, "Forbidden: staff only"));
            return false;
        }
        return true;
    }
    private String toError(int status, String message) {
        ErrorResponse err = new ErrorResponse(status, statusText(status), message);
        return toJsonError(err);
    }
    private String statusText(int status) {
        return switch (status) {
            case 400 -> "Bad Request";
            case 401 -> "Unauthorized";
            case 403 -> "Forbidden";
            case 404 -> "Not Found";
            case 405 -> "Method Not Allowed";
            case 500 -> "Internal Server Error";
            default -> "Error";
        };
    }
    private String toJsonFromProduct(Product product) {
        ProductDTO prod = toDTO(product);
        String categoryName = product.getCategory() != null ? escapeJson(product.getCategory().getName()) : "";
        String name = prod.name() != null ? escapeJson(prod.name()) : "";
        String type = prod.type() != null ? escapeJson(prod.type()) : "";
        String size = prod.size() != null ? escapeJson(prod.size()) : "";
        String gender = prod.gender() != null ? escapeJson(prod.gender()) : "";
        String imageUrl = prod.imageUrl() != null ? escapeJson(normalizeImageUrl(prod.imageUrl())) : "";
        String price = String.format(Locale.US, "%.2f", prod.price());
        String discountPercent = prod.discountPercent() != null ? String.format(Locale.US, "%.2f", prod.discountPercent()) : "null";
        String discountAmount = prod.discountAmount() != null ? String.format(Locale.US, "%.2f", prod.discountAmount()) : "null";
        return String.format("{\"id\":%d,\"name\":\"%s\",\"price\":%s,\"categoryId\":%d,\"categoryName\":\"%s\",\"type\":\"%s\",\"size\":\"%s\",\"gender\":\"%s\",\"imageUrl\":\"%s\",\"discountPercent\":%s,\"discountAmount\":%s}",
                prod.id(), name, price, prod.categoryId(), categoryName, type, size, gender, imageUrl, discountPercent, discountAmount);
    }

    private String toJsonArrayFromProducts(List<Product> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); ++i) {
            if (i > 0) sb.append(",");
            sb.append(toJsonFromProduct(list.get(i)));
        }
        sb.append("]");
        return sb.toString();
    }
    private ProductDTO readDtoFromBody(InputStream is) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        StringBuilder body = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            body.append(line);
        }
        String bodyStr = body.toString();
        if (bodyStr == null || bodyStr.isEmpty()) bodyStr = "";
        
        Map<String, String> params = new HashMap<>();
        String[] parts = bodyStr.split("&");
        for (String part : parts) {
            if (part.isEmpty()) continue;
            int eqIndex = part.indexOf("=");
            if (eqIndex == -1) continue;
            String key = URLDecoder.decode(part.substring(0, eqIndex), "UTF-8");
            String value = URLDecoder.decode(part.substring(eqIndex + 1), "UTF-8");
            params.put(key, value);
        }
        
        long id = 0;
        try {
            String idStr = params.getOrDefault("id", "0");
            if (!idStr.isBlank()) {
                id = Long.parseLong(idStr);
            }
        } catch (NumberFormatException e) {
        }
        
        String name = params.getOrDefault("name", "");
        double price = 0.0;
        try {
            String priceStr = params.getOrDefault("price", "0.0");
            if (!priceStr.isBlank()) {
                priceStr = priceStr.replace(",", ".");
                price = Double.parseDouble(priceStr);
            }
        } catch (NumberFormatException e) {
        }
        
        long categoryId = 0;
        try {
            String catIdStr = params.getOrDefault("categoryId", "0");
            if (!catIdStr.isBlank()) {
                categoryId = Long.parseLong(catIdStr);
            }
        } catch (NumberFormatException e) {
        }
        
        String type = params.getOrDefault("type", "");
        String size = params.getOrDefault("size", "");
        String gender = params.getOrDefault("gender", "");
        String imageUrl = normalizeImageUrl(params.getOrDefault("imageUrl", ""));
        
        Double discountPercent = null;
        try {
            String discountPercentStr = params.getOrDefault("discountPercent", "");
            if (!discountPercentStr.isBlank()) {
                discountPercentStr = discountPercentStr.replace(",", ".");
                discountPercent = Double.parseDouble(discountPercentStr);
            }
        } catch (NumberFormatException e) {
        }
        
        Double discountAmount = null;
        try {
            String discountAmountStr = params.getOrDefault("discountAmount", "");
            if (!discountAmountStr.isBlank()) {
                discountAmountStr = discountAmountStr.replace(",", ".");
                discountAmount = Double.parseDouble(discountAmountStr);
            }
        } catch (NumberFormatException e) {
        }
        
        return new ProductDTO(id, name, price, categoryId, type, size, gender, imageUrl, discountPercent, discountAmount);
    }
    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }
    private String normalizeImageUrl(String value) {
        if (value == null) return "";
        String url = value.trim().replace('\\', '/');
        if (url.isEmpty()) return "";
        if (url.startsWith("./assets/")) return url.substring(2);
        if (url.startsWith("/assets/")) return url.substring(1);
        if (url.startsWith("//")) return "https:" + url;
        if (url.toLowerCase(Locale.ROOT).startsWith("www.")) return "https://" + url;
        String lower = url.toLowerCase(Locale.ROOT);
        if (lower.startsWith("http://")
                && !lower.startsWith("http://localhost")
                && !lower.startsWith("http://127.0.0.1")
                && !lower.startsWith("http://0.0.0.0")) {
            return "https://" + url.substring(7);
        }
        return url;
    }
    private String toJsonError(ErrorResponse err) {
        return String.format("{\"status\":%d,\"error\":\"%s\",\"message\":\"%s\"}", err.status, escapeJson(err.error), escapeJson(err.message));
    }
}
