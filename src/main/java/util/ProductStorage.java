package util;

import model.Product;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ProductStorage {
    private static final String STORAGE_FILE = "products.json";

    public static void saveProducts(List<Product> products, long nextId) {
        try {
            StringBuilder json = new StringBuilder();
            json.append("{\n");
            json.append("  \"nextId\": ").append(nextId).append(",\n");
            json.append("  \"products\": [\n");
            
            for (int i = 0; i < products.size(); i++) {
                Product p = products.get(i);
                json.append("    {\n");
                json.append("      \"id\": ").append(p.getId()).append(",\n");
                json.append("      \"name\": \"").append(escapeJson(p.getName())).append("\",\n");
                json.append("      \"price\": ").append(String.format(Locale.US, "%.2f", p.getPrice())).append(",\n");
                json.append("      \"type\": \"").append(escapeJson(p.getType())).append("\",\n");
                json.append("      \"size\": \"").append(escapeJson(p.getSize())).append("\",\n");
                json.append("      \"gender\": \"").append(escapeJson(p.getGender())).append("\",\n");
                json.append("      \"imageUrl\": \"").append(escapeJson(p.getImageUrl())).append("\",\n");
                json.append("      \"discountPercent\": ").append(p.getDiscountPercent() != null ? String.format(Locale.US, "%.2f", p.getDiscountPercent()) : "null").append(",\n");
                json.append("      \"discountAmount\": ").append(p.getDiscountAmount() != null ? String.format(Locale.US, "%.2f", p.getDiscountAmount()) : "null").append("\n");
                json.append("    }");
                if (i < products.size() - 1) json.append(",");
                json.append("\n");
            }
            
            json.append("  ]\n");
            json.append("}\n");
            
            Files.write(Paths.get(STORAGE_FILE), json.toString().getBytes("UTF-8"));
        } catch (IOException e) {
            System.err.println("Помилка збереження товарів: " + e.getMessage());
        }
    }

    public static ProductData loadProducts() {
        try {
            File file = new File(STORAGE_FILE);
            if (!file.exists()) {
                return new ProductData(new ArrayList<>(), 1);
            }
            
            String json = new String(Files.readAllBytes(Paths.get(STORAGE_FILE)), "UTF-8");
            return parseJson(json);
        } catch (IOException e) {
            System.err.println("Помилка завантаження товарів: " + e.getMessage());
            return new ProductData(new ArrayList<>(), 1);
        }
    }

    private static ProductData parseJson(String json) {
        List<Product> products = new ArrayList<>();
        long nextId = 1;
        
        try {
            int nextIdStart = json.indexOf("\"nextId\"") + 9;
            int nextIdEnd = json.indexOf(",", nextIdStart);
            if (nextIdEnd == -1) nextIdEnd = json.indexOf("}", nextIdStart);
            if (nextIdStart > 8 && nextIdEnd > nextIdStart) {
                String nextIdStr = json.substring(nextIdStart, nextIdEnd).trim();
                nextId = Long.parseLong(nextIdStr);
            }
            
            int arrayStart = json.indexOf("\"products\"") + 11;
            int arrayBegin = json.indexOf("[", arrayStart);
            int arrayEnd = json.lastIndexOf("]");
            
            if (arrayBegin > 0 && arrayEnd > arrayBegin) {
                String productsJson = json.substring(arrayBegin + 1, arrayEnd);
                String[] productStrings = productsJson.split("\\},\\s*\\{");
                
                for (String productStr : productStrings) {
                    productStr = productStr.replaceAll("^\\{", "").replaceAll("\\}$", "");
                    Product product = parseProduct(productStr);
                    if (product != null) {
                        products.add(product);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Помилка парсингу JSON: " + e.getMessage());
        }
        
        return new ProductData(products, nextId);
    }

    private static Product parseProduct(String productJson) {
        try {
            long id = extractLong(productJson, "id");
            String name = extractString(productJson, "name");
            double price = extractDouble(productJson, "price");
            String type = extractString(productJson, "type");
            String size = extractString(productJson, "size");
            String gender = extractString(productJson, "gender");
            String imageUrl = extractString(productJson, "imageUrl");
            Double discountPercent = extractDoubleOrNull(productJson, "discountPercent");
            Double discountAmount = extractDoubleOrNull(productJson, "discountAmount");
            
            Product product = new Product();
            product.setId(id);
            product.setName(name);
            product.setPrice(price);
            product.setType(type);
            product.setSize(size);
            product.setGender(gender);
            product.setImageUrl(imageUrl);
            product.setDiscountPercent(discountPercent);
            product.setDiscountAmount(discountAmount);
            
            return product;
        } catch (Exception e) {
            System.err.println("Помилка парсингу товару: " + e.getMessage());
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

    private static double extractDouble(String json, String key) {
        int start = json.indexOf("\"" + key + "\"") + key.length() + 4;
        int end = json.indexOf(",", start);
        if (end == -1) end = json.indexOf("}", start);
        if (end == -1) end = json.indexOf("\n", start);
        if (end > start) {
            return Double.parseDouble(json.substring(start, end).trim());
        }
        return 0.0;
    }

    private static Double extractDoubleOrNull(String json, String key) {
        int start = json.indexOf("\"" + key + "\"") + key.length() + 4;
        int end = json.indexOf(",", start);
        if (end == -1) end = json.indexOf("}", start);
        if (end == -1) end = json.indexOf("\n", start);
        if (end > start) {
            String value = json.substring(start, end).trim();
            if (value.equals("null") || value.isEmpty()) {
                return null;
            }
            try {
                return Double.parseDouble(value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private static String extractString(String json, String key) {
        int start = json.indexOf("\"" + key + "\"") + key.length() + 4;
        if (json.charAt(start) == '"') start++;
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

    public static class ProductData {
        public List<Product> products;
        public long nextId;

        public ProductData() {
            this.products = new ArrayList<>();
            this.nextId = 1;
        }

        public ProductData(List<Product> products, long nextId) {
            this.products = products != null ? products : new ArrayList<>();
            this.nextId = nextId;
        }
    }
}
