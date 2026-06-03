package controller;

import com.sun.net.httpserver.*;
import service.UserService;
import util.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import model.User;
import dto.UserDTO;
import util.ValidationUtil;
import util.JwtUtil;
import util.PasswordUtil;
import util.EmailService;
import exception.ErrorResponse;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.net.URLDecoder;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

public class UserController implements HttpHandler {
    private final UserService userService;
    private final EmailService emailService;
    private static final ConcurrentHashMap<String, ResetRequest> RESET_REQUESTS = new ConcurrentHashMap<>();

    private static class ResetRequest {
        final String code;
        final LocalDateTime expiresAt;
        final String recoveryEmail;

        ResetRequest(String code, LocalDateTime expiresAt, String recoveryEmail) {
            this.code = code;
            this.expiresAt = expiresAt;
            this.recoveryEmail = recoveryEmail;
        }
    }

    public UserController(UserService userService) {
        this.userService = userService;
        this.emailService = new EmailService();
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
            if (method.equals("POST") && pathParts.length == 4 && "api".equals(pathParts[1]) && "users".equals(pathParts[2]) && "login".equals(pathParts[3])) {
                Map<String, String> loginData = readLoginData(exchange.getRequestBody());
                String email = loginData.get("email");
                String password = loginData.get("password");
                
                try {
                    ValidationUtil.validateLogin(email, password);
                } catch (IllegalArgumentException e) {
                    status = 400;
                    response = toError(400, e.getMessage());
                    sendResponse(exchange, status, response);
                    return;
                }
                
                User user = userService.findByEmail(email);
                if (user != null && PasswordUtil.matches(password, user.getPassword())) {
                    String token = JwtUtil.generateToken(user.getEmail(), user.getRole());
                    response = String.format(
                            "{\"token\":\"%s\",\"email\":\"%s\",\"role\":\"%s\",\"firstName\":\"%s\",\"lastName\":\"%s\",\"phone\":\"%s\",\"bonusBalance\":%s,\"bonusRate\":%s,\"spinCredits\":%d}",
                            escapeJson(token),
                            escapeJson(user.getEmail()),
                            escapeJson(user.getRole()),
                            escapeJson(user.getFirstName() != null ? user.getFirstName() : ""),
                            escapeJson(user.getLastName() != null ? user.getLastName() : ""),
                            escapeJson(user.getPhone() != null ? user.getPhone() : ""),
                            jsonNumber(user.getBonusBalance()),
                            user.getBonusRate() == null ? "null" : String.valueOf(user.getBonusRate()),
                            user.getSpinCredits()
                    );
                    status = 200;
                } else {
                    status = 401;
                    response = toError(401, "Невірний email або пароль. Перевірте дані та спробуйте ще раз.");
                }
            } else if (method.equals("GET") && pathParts.length == 3 && "api".equals(pathParts[1]) && "users".equals(pathParts[2])) {
                if (!isAdmin(exchange)) {
                    status = 403;
                    response = toError(403, "Недостатньо прав");
                    sendResponse(exchange, status, response);
                    return;
                }
                List<UserDTO> users = userService.getAll().stream().map(this::toSafeDTO).collect(Collectors.toList());
                response = toJsonArray(users);
            } else if (method.equals("GET") && pathParts.length == 4 && "api".equals(pathParts[1]) && "users".equals(pathParts[2]) && "me".equals(pathParts[3])) {
                String[] auth = parseAuth(exchange);
                if (auth == null || auth.length < 2) {
                    status = 401;
                    response = toError(401, "Unauthorized");
                    sendResponse(exchange, status, response);
                    return;
                }
                User u = userService.findByEmail(auth[0]);
                if (u == null) {
                    status = 404;
                    response = toError(404, "Not found");
                } else {
                    response = toJson(toSafeDTO(u));
                }
            } else if (method.equals("PUT") && pathParts.length == 4 && "api".equals(pathParts[1]) && "users".equals(pathParts[2]) && "me".equals(pathParts[3])) {
                String[] auth = parseAuth(exchange);
                if (auth == null || auth.length < 2) {
                    status = 401;
                    response = toError(401, "Unauthorized");
                    sendResponse(exchange, status, response);
                    return;
                }
                Map<String, String> data = readLoginData(exchange.getRequestBody());
                String firstName = data.getOrDefault("firstName", "").trim();
                String lastName = data.getOrDefault("lastName", "").trim();
                String phone = data.getOrDefault("phone", "").trim();
                if (firstName.isBlank() || lastName.isBlank()) {
                    status = 400;
                    response = toError(400, "Вкажіть ім'я та прізвище");
                    sendResponse(exchange, status, response);
                    return;
                }
                if (!phone.isBlank() && !phone.matches("^\\+380\\d{9}$")) {
                    status = 400;
                    response = toError(400, "Номер телефону має бути у форматі +380XXXXXXXXX");
                    sendResponse(exchange, status, response);
                    return;
                }
                User existing = userService.findByEmail(auth[0]);
                if (existing == null) {
                    status = 404;
                    response = toError(404, "Not found");
                    sendResponse(exchange, status, response);
                    return;
                }
                existing.setFirstName(firstName);
                existing.setLastName(lastName);
                if (phone.isBlank()) phone = existing.getPhone();
                existing.setPhone(phone);
                User toUpdate = new User(existing.getId(), existing.getEmail(), "", existing.getRole(), existing.getFirstName(), existing.getLastName(), existing.getBonusBalance(), existing.getBonusRate(), existing.getSpinCredits(), List.of());
                toUpdate.setPhone(existing.getPhone());
                User updated = userService.update(existing.getId(), toUpdate);
                response = toJson(toSafeDTO(updated));
            } else if (method.equals("PUT") && pathParts.length == 5 && "api".equals(pathParts[1]) && "users".equals(pathParts[2]) && "me".equals(pathParts[3]) && "password".equals(pathParts[4])) {
                String[] auth = parseAuth(exchange);
                if (auth == null || auth.length < 2) {
                    status = 401;
                    response = toError(401, "Unauthorized");
                    sendResponse(exchange, status, response);
                    return;
                }
                Map<String, String> data = readLoginData(exchange.getRequestBody());
                String currentPassword = data.getOrDefault("currentPassword", "");
                String newPassword = data.getOrDefault("newPassword", "");
                if (currentPassword.isBlank() || newPassword.isBlank()) {
                    status = 400;
                    response = toError(400, "Вкажіть поточний і новий пароль");
                    sendResponse(exchange, status, response);
                    return;
                }
                if (newPassword.trim().length() < 6) {
                    status = 400;
                    response = toError(400, "Новий пароль має бути мінімум 6 символів");
                    sendResponse(exchange, status, response);
                    return;
                }
                User existing = userService.findByEmail(auth[0]);
                if (existing == null) {
                    status = 404;
                    response = toError(404, "Not found");
                    sendResponse(exchange, status, response);
                    return;
                }
                if (!PasswordUtil.matches(currentPassword, existing.getPassword())) {
                    status = 400;
                    response = toError(400, "Невірний поточний пароль");
                    sendResponse(exchange, status, response);
                    return;
                }
                User toUpdate = new User(existing.getId(), existing.getEmail(), newPassword, existing.getRole(), existing.getFirstName(), existing.getLastName(), existing.getBonusBalance(), existing.getBonusRate(), existing.getSpinCredits(), List.of());
                toUpdate.setPhone(existing.getPhone());
                User updated = userService.update(existing.getId(), toUpdate);
                response = "{\"ok\":true}";
            } else if (method.equals("PUT") && pathParts.length == 5 && "api".equals(pathParts[1]) && "users".equals(pathParts[2]) && "me".equals(pathParts[3]) && "email".equals(pathParts[4])) {
                String[] auth = parseAuth(exchange);
                if (auth == null || auth.length < 2) {
                    status = 401;
                    response = toError(401, "Unauthorized");
                    sendResponse(exchange, status, response);
                    return;
                }
                Map<String, String> data = readLoginData(exchange.getRequestBody());
                String newEmail = data.getOrDefault("email", "").trim();
                String password = data.getOrDefault("password", "");
                if (newEmail.isBlank() || password.isBlank()) {
                    status = 400;
                    response = toError(400, "Вкажіть email і пароль");
                    sendResponse(exchange, status, response);
                    return;
                }
                if (!newEmail.matches(".+@.+\\..+")) {
                    status = 400;
                    response = toError(400, "Невірний формат email");
                    sendResponse(exchange, status, response);
                    return;
                }
                User existing = userService.findByEmail(auth[0]);
                if (existing == null) {
                    status = 404;
                    response = toError(404, "Not found");
                    sendResponse(exchange, status, response);
                    return;
                }
                if (!PasswordUtil.matches(password, existing.getPassword())) {
                    status = 400;
                    response = toError(400, "Невірний пароль");
                    sendResponse(exchange, status, response);
                    return;
                }
                User toUpdate = new User(existing.getId(), newEmail, "", existing.getRole(), existing.getFirstName(), existing.getLastName(), existing.getBonusBalance(), existing.getBonusRate(), existing.getSpinCredits(), List.of());
                toUpdate.setPhone(existing.getPhone());
                User updated = userService.update(existing.getId(), toUpdate);
                String token = JwtUtil.generateToken(updated.getEmail(), updated.getRole());
                response = String.format("{\"token\":\"%s\",\"email\":\"%s\",\"role\":\"%s\",\"firstName\":\"%s\",\"lastName\":\"%s\",\"phone\":\"%s\",\"bonusBalance\":%s,\"bonusRate\":%s,\"spinCredits\":%d}",
                        escapeJson(token),
                        escapeJson(updated.getEmail()),
                        escapeJson(updated.getRole()),
                        escapeJson(updated.getFirstName() != null ? updated.getFirstName() : ""),
                        escapeJson(updated.getLastName() != null ? updated.getLastName() : ""),
                        escapeJson(updated.getPhone() != null ? updated.getPhone() : ""),
                        jsonNumber(updated.getBonusBalance()),
                        updated.getBonusRate() == null ? "null" : String.valueOf(updated.getBonusRate()),
                        updated.getSpinCredits()
                );
            } else if (method.equals("POST")
                    && (
                        (pathParts.length == 5 && "api".equals(pathParts[1]) && "users".equals(pathParts[2]) && "password-reset".equals(pathParts[3]) && "request".equals(pathParts[4]))
                        || (pathParts.length == 6 && "api".equals(pathParts[1]) && "users".equals(pathParts[2]) && "password-reset".equals(pathParts[3]) && "request".equals(pathParts[4]) && (pathParts[5] == null || pathParts[5].isBlank()))
                    )) {
                Map<String, String> data = readLoginData(exchange.getRequestBody());
                String accountEmail = data.getOrDefault("accountEmail", data.getOrDefault("email", "")).trim();
                String recoveryEmail = data.getOrDefault("recoveryEmail", "").trim();
                if (recoveryEmail.isBlank()) recoveryEmail = accountEmail;
                response = "{\"ok\":true}";
                status = 200;
                if (!accountEmail.isBlank() && !recoveryEmail.isBlank()) {
                    User u = userService.findByEmail(accountEmail);
                    if (u != null) {
                        String code = String.valueOf(100000 + new java.util.Random().nextInt(900000));
                        LocalDateTime expires = LocalDateTime.now().plusMinutes(15);
                        RESET_REQUESTS.put(accountEmail.toLowerCase(), new ResetRequest(code, expires, recoveryEmail.toLowerCase()));
                        emailService.sendPasswordResetCode(recoveryEmail, code);
                    }
                }
            } else if (method.equals("POST")
                    && (
                        (pathParts.length == 5 && "api".equals(pathParts[1]) && "users".equals(pathParts[2]) && "password-reset".equals(pathParts[3]) && "confirm".equals(pathParts[4]))
                        || (pathParts.length == 6 && "api".equals(pathParts[1]) && "users".equals(pathParts[2]) && "password-reset".equals(pathParts[3]) && "confirm".equals(pathParts[4]) && (pathParts[5] == null || pathParts[5].isBlank()))
                    )) {
                Map<String, String> data = readLoginData(exchange.getRequestBody());
                String email = data.getOrDefault("accountEmail", data.getOrDefault("email", "")).trim();
                String code = data.getOrDefault("code", "").trim();
                String newPassword = data.getOrDefault("newPassword", "");
                if (email.isBlank() || code.isBlank() || newPassword.isBlank()) {
                    status = 400;
                    response = toError(400, "Заповніть email, код і новий пароль");
                    sendResponse(exchange, status, response);
                    return;
                }
                if (newPassword.trim().length() < 6) {
                    status = 400;
                    response = toError(400, "Новий пароль має бути мінімум 6 символів");
                    sendResponse(exchange, status, response);
                    return;
                }
                User u = userService.findByEmail(email);
                if (u == null) {
                    status = 400;
                    response = toError(400, "Невірний код або email");
                    sendResponse(exchange, status, response);
                    return;
                }
                ResetRequest rr = RESET_REQUESTS.get(email.toLowerCase());
                boolean ok = rr != null
                        && rr.code.equals(code)
                        && LocalDateTime.now().isBefore(rr.expiresAt);
                if (!ok) {
                    status = 400;
                    response = toError(400, "Невірний код або він прострочений");
                    sendResponse(exchange, status, response);
                    return;
                }
                User toUpdate = new User(u.getId(), u.getEmail(), newPassword, u.getRole(), u.getFirstName(), u.getLastName(), u.getBonusBalance(), u.getBonusRate(), u.getSpinCredits(), List.of());
                toUpdate.setPhone(u.getPhone());
                userService.update(u.getId(), toUpdate);
                RESET_REQUESTS.remove(email.toLowerCase());
                response = "{\"ok\":true}";
            } else if (method.equals("POST") && pathParts.length == 5 && "api".equals(pathParts[1]) && "users".equals(pathParts[2]) && "me".equals(pathParts[3]) && "bonus-spin".equals(pathParts[4])) {
                String[] auth = parseAuth(exchange);
                if (auth == null || auth.length < 2) {
                    status = 401;
                    response = toError(401, "Unauthorized");
                    sendResponse(exchange, status, response);
                    return;
                }
                User existing = userService.findByEmail(auth[0]);
                if (existing == null) {
                    status = 404;
                    response = toError(404, "Not found");
                    sendResponse(exchange, status, response);
                    return;
                }
                if (existing.getSpinCredits() <= 0) {
                    status = 400;
                    response = toError(400, "Немає доступних прокруток. Після кожної покупки нараховується 1 прокрутка.");
                    sendResponse(exchange, status, response);
                    return;
                }
                int rate = 2 + new java.util.Random().nextInt(4);
                existing.setBonusRate(rate);
                existing.setSpinCredits(Math.max(0, existing.getSpinCredits() - 1));
                User updated = userService.update(existing.getId(), new User(existing.getId(), existing.getEmail(), "", existing.getRole(), existing.getFirstName(), existing.getLastName(), existing.getBonusBalance(), existing.getBonusRate(), existing.getSpinCredits(), List.of()));
                response = String.format("{\"bonusRate\":%d,\"bonusBalance\":%s,\"spinCredits\":%d}", rate, jsonNumber(updated.getBonusBalance()), updated.getSpinCredits());
            } else if (method.equals("GET") && pathParts.length == 4 && "api".equals(pathParts[1]) && "users".equals(pathParts[2])) {
                if (!isAdmin(exchange)) {
                    status = 403;
                    response = toError(403, "Недостатньо прав");
                    sendResponse(exchange, status, response);
                    return;
                }
                long id = Long.parseLong(pathParts[3]);
                User u = userService.getById(id);
                if (u == null) { status = 404; response = toError(404, "Not found"); }
                else response = toJson(toSafeDTO(u));
            } else if (method.equals("POST") && pathParts.length == 3 && "api".equals(pathParts[1]) && "users".equals(pathParts[2])) {
                UserDTO dto = readDtoFromBody(exchange.getRequestBody());
                dto = new UserDTO(dto.id(), dto.email(), dto.password(), "USER", dto.firstName(), dto.lastName(), dto.phone(), dto.bonusBalance(), dto.bonusRate(), dto.spinCredits());
                
                try { 
                    ValidationUtil.validate(dto); 
                } catch (IllegalArgumentException e) {
                    status = 400; 
                    String errorMsg = e.getMessage();
                    if (errorMsg.contains("Role cannot be empty") || errorMsg.contains("Роль не може бути порожньою")) {
                        errorMsg = "Роль не може бути порожньою";
                    } else if (errorMsg.contains("Invalid email")) {
                        errorMsg = "Невірний формат email";
                    }
                    response = toError(400, errorMsg); 
                    sendResponse(exchange, status, response); 
                    return;
                }
                User user = fromDTO(dto);
                try {
                    User created = userService.create(user);
                    response = toJson(toDTO(created));
                    status = 201;
                } catch (IllegalArgumentException e) {
                    status = 400;
                    String errorMsg = e.getMessage();
                    if (errorMsg.contains("Email already exists") || errorMsg.contains("already exists")) {
                        errorMsg = "Цей email вже зареєстровано. Спробуйте інший email або увійдіть в систему.";
                    }
                    response = toError(400, errorMsg);
                }
            } else if (method.equals("PUT") && pathParts.length == 4 && "api".equals(pathParts[1]) && "users".equals(pathParts[2])) {
                if (!isAdmin(exchange)) {
                    status = 403;
                    response = toError(403, "Недостатньо прав");
                    sendResponse(exchange, status, response);
                    return;
                }
                long id = Long.parseLong(pathParts[3]);
                UserDTO dto = readDtoFromBody(exchange.getRequestBody());
                try { ValidationUtil.validate(dto); } catch (IllegalArgumentException e) {
                    status = 400; response = toError(400, e.getMessage()); sendResponse(exchange, status, response); return;
                }
                User user = fromDTO(dto);
                User existing = userService.getById(id);
                if (existing != null) {
                    if (user.getBonusRate() == null) user.setBonusRate(existing.getBonusRate());
                    user.setBonusBalance(existing.getBonusBalance());
                    user.setSpinCredits(existing.getSpinCredits());
                }
                try {
                    User updated = userService.update(id, user);
                    if (updated == null) { status = 404; response = toError(404, "Користувача не знайдено"); }
                    else response = toJson(toSafeDTO(updated));
                } catch (IllegalArgumentException e) {
                    status = 400;
                    String errorMsg = e.getMessage();
                    if (errorMsg.contains("Email already exists") || errorMsg.contains("already exists")) {
                        errorMsg = "Цей email вже використовується іншим користувачем";
                    }
                    response = toError(400, errorMsg);
                }
            } else if (method.equals("PUT") && pathParts.length == 5 && "api".equals(pathParts[1]) && "users".equals(pathParts[2]) && "role".equals(pathParts[4])) {
                if (!isAdmin(exchange)) {
                    status = 403;
                    response = toError(403, "Недостатньо прав");
                    sendResponse(exchange, status, response);
                    return;
                }
                long id = Long.parseLong(pathParts[3]);
                Map<String, String> roleData = readLoginData(exchange.getRequestBody());
                String role = roleData.getOrDefault("role", "").trim();
                String normalizedRole = role.toUpperCase(java.util.Locale.ROOT);
                if (!"ADMIN".equals(normalizedRole) && !"USER".equals(normalizedRole)) {
                    status = 400;
                    response = toError(400, "Невірна роль");
                    sendResponse(exchange, status, response);
                    return;
                }
                role = normalizedRole;
                String[] auth = parseAuth(exchange);
                User existing = userService.getById(id);
                if (existing == null) {
                    status = 404;
                    response = toError(404, "Користувача не знайдено");
                    sendResponse(exchange, status, response);
                    return;
                }
                if (auth != null && auth.length >= 1 && existing.getEmail() != null && existing.getEmail().equalsIgnoreCase(auth[0]) && "ADMIN".equalsIgnoreCase(existing.getRole()) && !"ADMIN".equalsIgnoreCase(role)) {
                    status = 400;
                    response = toError(400, "Неможливо забрати права адміністратора у себе");
                    sendResponse(exchange, status, response);
                    return;
                }
                User toUpdate = new User(
                        existing.getId(),
                        existing.getEmail(),
                        "",
                        role,
                        existing.getFirstName(),
                        existing.getLastName(),
                        existing.getBonusBalance(),
                        existing.getBonusRate(),
                        existing.getSpinCredits(),
                        List.of()
                );
                User updated = userService.update(id, toUpdate);
                if (updated == null) {
                    status = 404;
                    response = toError(404, "Користувача не знайдено");
                } else {
                    response = toJson(toSafeDTO(updated));
                }
            } else if (method.equals("DELETE") && pathParts.length == 4 && "api".equals(pathParts[1]) && "users".equals(pathParts[2])) {
                if (!isAdmin(exchange)) {
                    status = 403;
                    response = toError(403, "Недостатньо прав");
                    sendResponse(exchange, status, response);
                    return;
                }
                long id = Long.parseLong(pathParts[3]);
                boolean removed = userService.delete(id);
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
    
    private Map<String, String> readLoginData(InputStream is) throws IOException {
        Map<String, String> data = new java.util.HashMap<>();
        BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        String line = br.readLine();
        if (line == null) line = "";
        String[] parts = line.split("&");
        for (String part : parts) {
            String[] kv = part.split("=", 2);
            if (kv.length == 2) {
                String key = URLDecoder.decode(kv[0], "UTF-8");
                String value = URLDecoder.decode(kv[1], "UTF-8");
                data.put(key, value);
            }
        }
        return data;
    }
    private UserDTO toDTO(User u) {
        return new UserDTO(u.getId(), u.getEmail(), u.getPassword(), u.getRole(), u.getFirstName(), u.getLastName(), u.getPhone(), u.getBonusBalance(), u.getBonusRate(), u.getSpinCredits());
    }
    private UserDTO toSafeDTO(User u) {
        return new UserDTO(u.getId(), u.getEmail(), "", u.getRole(), u.getFirstName(), u.getLastName(), u.getPhone(), u.getBonusBalance(), u.getBonusRate(), u.getSpinCredits());
    }
    private User fromDTO(UserDTO dto) {
        User u = new User(dto.id(), dto.email(), dto.password(), dto.role(), dto.firstName(), dto.lastName(), dto.bonusBalance(), dto.bonusRate(), dto.spinCredits(), List.of());
        u.setPhone(dto.phone());
        return u;
    }
    private void sendResponse(HttpExchange exchange, int status, String response) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(status, response.getBytes(StandardCharsets.UTF_8).length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes(StandardCharsets.UTF_8));
        }
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
    private String toJson(UserDTO user) {
        return String.format("{\"id\":%d,\"email\":\"%s\",\"password\":\"%s\",\"role\":\"%s\",\"firstName\":\"%s\",\"lastName\":\"%s\",\"phone\":\"%s\",\"bonusBalance\":%s,\"bonusRate\":%s,\"spinCredits\":%d}",
                user.id(), escapeJson(user.email()), escapeJson(user.password()), escapeJson(user.role()),
                escapeJson(user.firstName() != null ? user.firstName() : ""),
                escapeJson(user.lastName() != null ? user.lastName() : ""),
                escapeJson(user.phone() != null ? user.phone() : ""),
                jsonNumber(user.bonusBalance()),
                user.bonusRate() == null ? "null" : String.valueOf(user.bonusRate()),
                user.spinCredits());
    }
    private String toJsonArray(List<UserDTO> list) {
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
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
    private String toJsonError(ErrorResponse err) {
        return String.format("{\"status\":%d,\"error\":\"%s\",\"message\":\"%s\"}", err.status, escapeJson(err.error), escapeJson(err.message));
    }
    private String jsonNumber(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) return "0";
        return java.math.BigDecimal.valueOf(value).setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }
    private UserDTO readDtoFromBody(InputStream is) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        String line = br.readLine();
        if (line == null) line = "";
        String[] parts = line.split("&");
        String email = "", password = "", role = "USER";
        String firstName = "", lastName = "", phone = "";
        long id = 0L;
        for (String part : parts) {
            int eqIndex = part.indexOf("=");
            if (eqIndex == -1) continue;
            String key = URLDecoder.decode(part.substring(0, eqIndex), "UTF-8");
            String value = URLDecoder.decode(part.substring(eqIndex + 1), "UTF-8");
            switch (key) {
                case "id" -> {
                    if (!value.isBlank()) {
                        try {
                            id = Long.parseLong(value);
                        } catch (NumberFormatException e) {
                        }
                    }
                }
                case "email" -> email = value;
                case "password" -> password = value;
                case "firstName" -> firstName = value;
                case "lastName" -> lastName = value;
                case "phone" -> phone = value;
                case "role" -> {
                    if (!value.isBlank()) {
                        role = value;
                    }
                }
            }
        }
        if (role == null || role.isEmpty()) {
            role = "USER";
        }
        return new UserDTO(id, email, password, role, firstName, lastName, phone, 0.0, null, 0);
    }

    private String[] parseAuth(HttpExchange exchange) {
        String auth = exchange.getRequestHeaders().getFirst("Authorization");
        if (auth == null || auth.isBlank()) return null;
        String value = auth.trim();
        if (value.toLowerCase().startsWith("bearer ")) {
            value = value.substring(7).trim();
        }
        if (value.isBlank()) return null;
        return JwtUtil.validateToken(value);
    }

    private boolean isAdmin(HttpExchange exchange) {
        String[] data = parseAuth(exchange);
        return data != null && data.length >= 2 && "ADMIN".equalsIgnoreCase(data[1]);
    }

    private boolean hasAnyOrders(long userId) {
        String sql = "SELECT COUNT(*) FROM orders WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1) > 0;
                }
            }
        } catch (Exception e) {
            System.err.println("Помилка перевірки замовлень користувача: " + e.getMessage());
        }
        return false;
    }
}
