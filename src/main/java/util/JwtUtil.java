package util;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class JwtUtil {
    private static final Map<String, String[]> tokenStore = new HashMap<>();
    
    public static String generateToken(String email, String role) {
        String token = Base64.getEncoder().encodeToString((email + ":" + role + ":" + System.currentTimeMillis()).getBytes());
        tokenStore.put(token, new String[]{email, role});
        return token;
    }
    
    public static String[] validateToken(String token) {
        return tokenStore.get(token);
    }
}
