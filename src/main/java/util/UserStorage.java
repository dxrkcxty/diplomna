package util;

import model.User;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class UserStorage {
    private static final String STORAGE_FILE = "users.json";

    public static void saveUsers(List<User> users, long nextId) {
        try {
            StringBuilder json = new StringBuilder();
            json.append("{\n");
            json.append("  \"nextId\": ").append(nextId).append(",\n");
            json.append("  \"users\": [\n");
            
            for (int i = 0; i < users.size(); i++) {
                User u = users.get(i);
                json.append("    {\n");
                json.append("      \"id\": ").append(u.getId()).append(",\n");
                json.append("      \"email\": \"").append(escapeJson(u.getEmail())).append("\",\n");
                json.append("      \"password\": \"").append(escapeJson(u.getPassword())).append("\",\n");
                json.append("      \"role\": \"").append(escapeJson(u.getRole())).append("\"\n");
                json.append("    }");
                if (i < users.size() - 1) json.append(",");
                json.append("\n");
            }
            
            json.append("  ]\n");
            json.append("}\n");
            
            Files.write(Paths.get(STORAGE_FILE), json.toString().getBytes("UTF-8"));
        } catch (IOException e) {
            System.err.println("Помилка збереження користувачів: " + e.getMessage());
        }
    }

    public static UserData loadUsers() {
        try {
            File file = new File(STORAGE_FILE);
            if (!file.exists()) {
                return new UserData(new ArrayList<>(), 1);
            }
            
            String json = new String(Files.readAllBytes(Paths.get(STORAGE_FILE)), "UTF-8");
            return parseJson(json);
        } catch (IOException e) {
            System.err.println("Помилка завантаження користувачів: " + e.getMessage());
            return new UserData(new ArrayList<>(), 1);
        }
    }

    private static UserData parseJson(String json) {
        List<User> users = new ArrayList<>();
        long nextId = 1;
        
        try {
            int nextIdStart = json.indexOf("\"nextId\"") + 9;
            int nextIdEnd = json.indexOf(",", nextIdStart);
            if (nextIdEnd == -1) nextIdEnd = json.indexOf("}", nextIdStart);
            if (nextIdStart > 8 && nextIdEnd > nextIdStart) {
                String nextIdStr = json.substring(nextIdStart, nextIdEnd).trim();
                nextId = Long.parseLong(nextIdStr);
            }
            
            int arrayStart = json.indexOf("\"users\"") + 9;
            int arrayBegin = json.indexOf("[", arrayStart);
            int arrayEnd = json.lastIndexOf("]");
            
            if (arrayBegin > 0 && arrayEnd > arrayBegin) {
                String usersJson = json.substring(arrayBegin + 1, arrayEnd);
                String[] userStrings = usersJson.split("\\},\\s*\\{");
                
                for (String userStr : userStrings) {
                    userStr = userStr.replaceAll("^\\{", "").replaceAll("\\}$", "");
                    User user = parseUser(userStr);
                    if (user != null) {
                        users.add(user);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Помилка парсингу JSON: " + e.getMessage());
        }
        
        return new UserData(users, nextId);
    }

    private static User parseUser(String userJson) {
        try {
            long id = extractLong(userJson, "id");
            String email = extractString(userJson, "email");
            String password = extractString(userJson, "password");
            String role = extractString(userJson, "role");
            
            User user = new User();
            user.setId(id);
            user.setEmail(email);
            user.setPassword(password);
            user.setRole(role);
            
            return user;
        } catch (Exception e) {
            System.err.println("Помилка парсингу користувача: " + e.getMessage());
            return null;
        }
    }

    private static long extractLong(String json, String key) {
        int start = json.indexOf("\"" + key + "\"") + key.length() + 4;
        int end = json.indexOf(",", start);
        if (end == -1) end = json.indexOf("}", start);
        if (end == -1) end = json.indexOf("\n", start);
        if (end > start) {
            return Long.parseLong(json.substring(start, end).trim());
        }
        return 0;
    }

    private static String extractString(String json, String key) {
        int start = json.indexOf("\"" + key + "\"") + key.length() + 4;
        if (start < key.length() + 4) return "";
        if (json.length() > start && json.charAt(start) == '"') start++;
        int end = json.indexOf("\"", start);
        if (end == -1) end = json.indexOf(",", start);
        if (end == -1) end = json.indexOf("}", start);
        if (end == -1) end = json.indexOf("\n", start);
        if (end > start) {
            String value = json.substring(start, end).trim();
            return unescapeJson(value.replaceAll("^\"|\"$", ""));
        }
        return "";
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
                   .replace("\\\\", "\\")
                   .replace("\\n", "\n")
                   .replace("\\r", "\r")
                   .replace("\\t", "\t");
    }

    public static class UserData {
        public List<User> users;
        public long nextId;

        public UserData() {
            this.users = new ArrayList<>();
            this.nextId = 1;
        }

        public UserData(List<User> users, long nextId) {
            this.users = users != null ? users : new ArrayList<>();
            this.nextId = nextId;
        }
    }
}

