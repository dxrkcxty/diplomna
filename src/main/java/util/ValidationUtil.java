package util;

import dto.*;
import service.UserService;
import java.util.regex.Pattern;
import java.util.List;

public class ValidationUtil {
    public static void validate(ProductDTO dto) {
        if (dto.name() == null || dto.name().trim().isEmpty()) throw new IllegalArgumentException("Product name cannot be empty");
        if (dto.price() < 0) throw new IllegalArgumentException("Product price cannot be negative");
    }
    public static void validate(CategoryDTO dto) {
        if (dto.name() == null || dto.name().trim().isEmpty()) throw new IllegalArgumentException("Category name cannot be empty");
    }
    public static void validate(UserDTO dto) {
        if (dto.email() == null || !Pattern.matches(".+@.+\\..+", dto.email())) throw new IllegalArgumentException("Invalid email");
        if (dto.role() == null || dto.role().isEmpty()) throw new IllegalArgumentException("Роль не може бути порожньою");
    }
    
    public static void validateLogin(String email, String password) {
        if (email == null || !Pattern.matches(".+@.+\\..+", email)) throw new IllegalArgumentException("Невірний email");
        if (password == null || password.isEmpty()) throw new IllegalArgumentException("Пароль не може бути порожнім");
    }
    public static void validate(UserDTO dto, UserService userService) {
        validate(dto);
        if (userService.existsByEmail(dto.email())) throw new IllegalArgumentException("Email already exists");
    }
    public static void validate(OrderDTO dto) {
        if (dto.userId() <= 0) throw new IllegalArgumentException("UserId must be positive");
        if (dto.productIds() == null || dto.productIds().isEmpty()) throw new IllegalArgumentException("Order must have products");
        if (dto.status() == null || dto.status().isEmpty()) throw new IllegalArgumentException("Order status required");
    }
    public static void validate(ReviewDTO dto) {
        if (dto.userId() <= 0) throw new IllegalArgumentException("UserId required");
        if (dto.productId() < 0) throw new IllegalArgumentException("ProductId invalid");
        if (dto.rating() < 1 || dto.rating() > 5) throw new IllegalArgumentException("Rating must be 1-5");
        if (dto.comment() != null && dto.comment().length() > 500) throw new IllegalArgumentException("Comment too long");
    }
}