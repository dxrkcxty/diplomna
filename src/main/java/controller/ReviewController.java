package controller;

import com.sun.net.httpserver.*;
import service.ReviewService;
import service.UserService;
import service.ProductService;
import service.OrderService;
import model.Review;
import model.User;
import model.Product;
import dto.ReviewDTO;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import util.ValidationUtil;
import util.JwtUtil;
import exception.ErrorResponse;

public class ReviewController implements HttpHandler {
    private final ReviewService reviewService;
    private final UserService userService;
    private final ProductService productService;
    private final OrderService orderService;

    public ReviewController(ReviewService reviewService, UserService userService, ProductService productService, OrderService orderService) {
        this.reviewService = reviewService;
        this.userService = userService;
        this.productService = productService;
        this.orderService = orderService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        if (method.equals("OPTIONS")) {
            setCorsHeaders(exchange);
            exchange.sendResponseHeaders(200, -1);
            return;
        }
        
        String path = exchange.getRequestURI().getPath();
        String[] pathParts = path.split("/");
        URI uri = exchange.getRequestURI();
        Map<String, String> queryParams = parseQueryParams(uri);
        String response = "";
        int status = 200;
        
        try {
            if (method.equals("GET") && pathParts.length == 3 && "api".equals(pathParts[1]) && "reviews".equals(pathParts[2])) {
                if (queryParams.containsKey("productId")) {
                    long productId = Long.parseLong(queryParams.get("productId"));
                    List<Review> reviews = reviewService.getByProductId(productId);
                    response = toJsonArrayWithEmail(reviews);
                } else if (queryParams.containsKey("orderedProducts")) {
                    String[] authData = checkAuthAndGetUser(exchange);
                    if (authData == null) return;
                    String email = authData[0];
                    User user = userService.findByEmail(email);
                    if (user == null) {
                        status = 401; response = toError(401, "Unknown user");
                        sendResponse(exchange, status, response); return;
                    }
                    long userId = user.getId();
                    List<Product> orderedProducts = productService.getOrderedByUserId(userId);
                    StringBuilder sb = new StringBuilder("[");
                    for (int i = 0; i < orderedProducts.size(); i++) {
                        if (i > 0) sb.append(",");
                        Product p = orderedProducts.get(i);
                        sb.append(String.format("{\"id\":%d,\"name\":\"%s\"}", p.getId(), escapeJson(p.getName() != null ? p.getName() : "")));
                    }
                    sb.append("]");
                    response = sb.toString();
                } else {
                    List<Review> reviews = reviewService.getAll();
                    response = toJsonArrayWithEmail(reviews);
                }
            } else if (method.equals("GET") && pathParts.length == 4 && "api".equals(pathParts[1]) && "reviews".equals(pathParts[2])) {
                long id = Long.parseLong(pathParts[3]);
                Review r = reviewService.getById(id);
                if (r == null) { status = 404; response = toError(404, "Not found"); }
                else response = toJsonWithEmail(r);
                } else if (method.equals("POST") && pathParts.length == 3 && "api".equals(pathParts[1]) && "reviews".equals(pathParts[2])) {
                    String[] authData = checkAuthAndGetUser(exchange);
                    if (authData == null) return;
                    String email = authData[0];
                    User user = userService.findByEmail(email);
                    if (user == null) {
                        status = 401; response = toError(401, "Unknown user");
                        sendResponse(exchange, status, response); return;
                    }
                    long userId = user.getId();
                    ReviewDTO dto = readDtoFromBody(exchange.getRequestBody());
                    long productId = dto.productId();
                    if (productId > 0) {
                        Product p = productService.getById(productId);
                        if (p == null) {
                            status = 400;
                            response = toError(400, "Товар не знайдено");
                            sendResponse(exchange, status, response);
                            return;
                        }
                        if (!orderService.hasUserOrderedProduct(userId, productId)) {
                            status = 400;
                            response = toError(400, "Відгук про товар можна залишити лише після покупки");
                            sendResponse(exchange, status, response);
                            return;
                        }
                    }
                    dto = new ReviewDTO(dto.id(), userId, productId, dto.rating(), dto.comment());
                    
                    try { ValidationUtil.validate(dto); } catch (IllegalArgumentException e) {
                        status = 400; response = toError(400, e.getMessage()); sendResponse(exchange, status, response); return;
                    }
                    Review review = fromDTO(dto);
                    Review created = reviewService.create(review);
                    response = toJsonWithEmail(created);
                    status = 201;
            } else if (method.equals("PUT") && pathParts.length == 4 && "api".equals(pathParts[1]) && "reviews".equals(pathParts[2])) {
                if (!checkAuth(exchange, true)) return;
                long id = Long.parseLong(pathParts[3]);
                ReviewDTO dto = readDtoFromBody(exchange.getRequestBody());
                try { ValidationUtil.validate(dto); } catch (IllegalArgumentException e) {
                    status = 400; response = toError(400, e.getMessage()); sendResponse(exchange, status, response); return;
                }
                Review review = fromDTO(dto);
                Review updated = reviewService.update(id, review);
                if (updated == null) { status = 404; response = toError(404, "Not found"); }
                else response = toJsonWithEmail(updated);
            } else if (method.equals("DELETE") && pathParts.length == 4 && "api".equals(pathParts[1]) && "reviews".equals(pathParts[2])) {
                if (!checkAuth(exchange, true)) return;
                long id = Long.parseLong(pathParts[3]);
                boolean removed = reviewService.delete(id);
                if (removed) { status = 204; response = ""; }
                else { status = 404; response = toError(404, "Not found"); }
            } else {
                status = 405; response = toError(405, "Method Not Allowed");
            }
        } catch(Exception ex) {
            status = 500; response = toError(500, ex.getMessage());
            System.err.println("Помилка в ReviewController: " + ex.getMessage());
            ex.printStackTrace();
        }
        setCorsHeaders(exchange);
        sendResponse(exchange, status, response);
    }
    private ReviewDTO toDTO(Review r) {
        long userId = (r.getUser() != null) ? r.getUser().getId() : 0L;
        long productId = (r.getProduct() != null) ? r.getProduct().getId() : 0L;
        return new ReviewDTO(r.getId(), userId, productId, r.getRating(), r.getComment());
    }
    
    private Review fromDTO(ReviewDTO dto) {
        User user = userService.getById(dto.userId());
        Product product = (dto.productId() > 0) ? productService.getById(dto.productId()) : null;
        Review review = new Review();
        review.setId(dto.id());
        review.setUser(user);
        review.setProduct(product);
        review.setRating(dto.rating());
        review.setComment(dto.comment());
        return review;
    }
    
    private String toJsonWithEmail(Review review) {
        String userEmail = review.getUser() != null ? review.getUser().getEmail() : "Невідомий";
        long productId = review.getProduct() != null ? review.getProduct().getId() : 0;
        String productName = review.getProduct() != null ? review.getProduct().getName() : "";
        String subjectType = productId > 0 ? "PRODUCT" : "SITE";
        return String.format("{\"id\":%d,\"userId\":%d,\"productId\":%d,\"productName\":\"%s\",\"subjectType\":\"%s\",\"rating\":%d,\"comment\":\"%s\",\"userEmail\":\"%s\"}",
                review.getId(),
                review.getUser() != null ? review.getUser().getId() : 0,
                productId,
                escapeJson(productName),
                escapeJson(subjectType),
                review.getRating(),
                escapeJson(review.getComment()),
                escapeJson(userEmail));
    }
    
    private String toJsonArrayWithEmail(List<Review> reviews) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < reviews.size(); ++i) {
            if (i > 0) sb.append(",");
            sb.append(toJsonWithEmail(reviews.get(i)));
        }
        sb.append("]");
        return sb.toString();
    }
    
    private Map<String, String> parseQueryParams(URI uri) {
        Map<String, String> map = new java.util.HashMap<>();
        String q = uri.getQuery();
        if (q != null) {
            for (String param : q.split("&")) {
                String[] parts = param.split("=", 2);
                if (parts.length == 2) {
                    try {
                        map.put(parts[0], java.net.URLDecoder.decode(parts[1], StandardCharsets.UTF_8));
                    } catch (Exception e) {
                        map.put(parts[0], parts[1]);
                    }
                }
            }
        }
        return map;
    }
    
    private String[] checkAuthAndGetUser(HttpExchange exchange) throws IOException {
        String auth = exchange.getRequestHeaders().getFirst("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            sendResponse(exchange, 401, toError(401, "Auth required"));
            return null;
        }
        String token = auth.substring(7);
        String[] data = JwtUtil.validateToken(token);
        if (data == null) {
            sendResponse(exchange, 401, toError(401, "Invalid token"));
            return null;
        }
        return data;
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
    
    private void setCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
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
            sendResponse(exchange, 403, toError(403, "Forbidden: admin only"));
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
    private String toJson(ReviewDTO review) {
        return String.format("{\"id\":%d,\"userId\":%d,\"productId\":%d,\"rating\":%d,\"comment\":\"%s\"}",
                review.id(), review.userId(), review.productId(), review.rating(), escapeJson(review.comment()));
    }
    private String toJsonArray(List<ReviewDTO> list) {
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
    private String toJsonError(ErrorResponse err) {
        return String.format("{\"status\":%d,\"error\":\"%s\",\"message\":\"%s\"}", err.status, escapeJson(err.error), escapeJson(err.message));
    }
    private ReviewDTO readDtoFromBody(InputStream is) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        String line = br.readLine();
        if (line == null) line = "";
        String[] parts = line.split("&");
        long id = 0L, userId = 0L, productId = 0L;
        int rating = 0;
        String comment = "";
        for (String part : parts) {
            String[] kv = part.split("=", 2);
            if (kv.length != 2) continue;
            String key = kv[0];
            String val = java.net.URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
            switch (key) {
                case "id" -> id = val.isBlank() ? 0 : Long.parseLong(val);
                case "userId" -> userId = val.isBlank() ? 0 : Long.parseLong(val);
                case "productId" -> productId = val.isBlank() ? 0 : Long.parseLong(val);
                case "rating" -> rating = val.isBlank() ? 0 : Integer.parseInt(val);
                case "comment" -> comment = val;
            }
        }
        return new ReviewDTO(id, userId, productId, rating, comment);
    }
}
