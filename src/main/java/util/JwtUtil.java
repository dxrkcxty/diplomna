package util;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class JwtUtil {
    private static final Map<String, String[]> tokenStore = new HashMap<>();
    
    public static String generateToken(String email, String role) {
        String token = Base64.getUrlEncoder().withoutPadding()
                .encodeToString((email + ":" + role + ":" + System.currentTimeMillis()).getBytes(StandardCharsets.UTF_8));
        tokenStore.put(token, new String[]{email, role});
        return token;
    }
    
    public static String[] validateToken(String token) {
        String[] stored = tokenStore.get(token);
        if (stored != null) {
            return stored;
        }

        try {
            String decoded = new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);
            String[] parts = decoded.split(":", 3);
            if (parts.length >= 2 && !parts[0].isBlank() && !parts[1].isBlank()) {
                return new String[]{parts[0], parts[1]};
            }
        } catch (Exception ignored) {
        }

        try {
            String decoded = new String(Base64.getDecoder().decode(token), StandardCharsets.UTF_8);
            String[] parts = decoded.split(":", 3);
            if (parts.length >= 2 && !parts[0].isBlank() && !parts[1].isBlank()) {
                return new String[]{parts[0], parts[1]};
            }
        } catch (Exception ignored) {
        }

        return null;
    }
}
