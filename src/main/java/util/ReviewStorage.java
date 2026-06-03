package util;

import model.Review;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class ReviewStorage {
    private static final Path STORAGE_PATH = Paths.get("reviews.json");

    public static class ReviewData {
        public long nextId;
        public List<Review> reviews;
        public List<ReviewDataItem> reviewItems;

        public ReviewData(long nextId, List<Review> reviews) {
            this.nextId = nextId;
            this.reviews = reviews;
            this.reviewItems = new java.util.ArrayList<>();
        }
        
        public ReviewData(long nextId, List<Review> reviews, List<ReviewDataItem> reviewItems) {
            this.nextId = nextId;
            this.reviews = reviews;
            this.reviewItems = reviewItems;
        }
    }

    public static void saveReviews(List<Review> reviews, long nextId) {
        try {
            if (STORAGE_PATH.getParent() != null) {
                Files.createDirectories(STORAGE_PATH.getParent());
            }
            StringBuilder json = new StringBuilder();
            json.append("{\n");
            json.append("  \"nextId\": ").append(nextId).append(",\n");
            json.append("  \"reviews\": [\n");
            
            for (int i = 0; i < reviews.size(); i++) {
                Review r = reviews.get(i);
                json.append("    {\n");
                json.append("      \"id\": ").append(r.getId()).append(",\n");
                json.append("      \"userId\": ").append(r.getUser() != null ? r.getUser().getId() : 0).append(",\n");
                json.append("      \"productId\": ").append(r.getProduct() != null ? r.getProduct().getId() : 0).append(",\n");
                json.append("      \"rating\": ").append(r.getRating()).append(",\n");
                json.append("      \"comment\": \"").append(escapeJson(r.getComment())).append("\"\n");
                json.append("    }");
                if (i < reviews.size() - 1) json.append(",");
                json.append("\n");
            }
            
            json.append("  ]\n");
            json.append("}\n");
            
            Files.write(STORAGE_PATH, json.toString().getBytes("UTF-8"));
        } catch (IOException e) {
            System.err.println("Помилка збереження відгуків: " + e.getMessage());
        }
    }

    public static ReviewData loadReviews() {
        try {
            File file = STORAGE_PATH.toFile();
            if (!file.exists() || file.length() == 0) {
                return new ReviewData(1, new ArrayList<>(), new ArrayList<>());
            }
            
            String content = new String(Files.readAllBytes(STORAGE_PATH), "UTF-8");
            return parseJson(content);
        } catch (IOException e) {
            System.err.println("Помилка завантаження відгуків: " + e.getMessage());
            return new ReviewData(1, new ArrayList<>(), new ArrayList<>());
        }
    }

    private static ReviewData parseJson(String json) {
        long nextId = 1;
        List<Review> reviews = new ArrayList<>();
        List<ReviewDataItem> reviewItems = new ArrayList<>();
        
        try {
            int nextIdStart = json.indexOf("\"nextId\":");
            if (nextIdStart != -1) {
                int nextIdEnd = json.indexOf(",", nextIdStart);
                if (nextIdEnd == -1) nextIdEnd = json.indexOf("}", nextIdStart);
                String nextIdStr = json.substring(nextIdStart + 9, nextIdEnd).trim();
                nextId = Long.parseLong(nextIdStr);
            }
            
            int reviewsStart = json.indexOf("\"reviews\":[");
            if (reviewsStart == -1) {
                return new ReviewData(nextId, reviews, reviewItems);
            }
            
            int pos = reviewsStart + 11;
            while (pos < json.length()) {
                int objStart = json.indexOf("{", pos);
                if (objStart == -1) break;
                
                int braceCount = 0;
                int objEnd = objStart;
                for (int i = objStart; i < json.length(); i++) {
                    if (json.charAt(i) == '{') braceCount++;
                    if (json.charAt(i) == '}') braceCount--;
                    if (braceCount == 0) {
                        objEnd = i;
                        break;
                    }
                }
                
                String reviewJson = json.substring(objStart, objEnd + 1);
                long userId = extractLong(reviewJson, "userId");
                long productId = extractLong(reviewJson, "productId");
                Review review = parseReview(reviewJson);
                if (review != null) {
                    reviews.add(review);
                    reviewItems.add(new ReviewDataItem(review, userId, productId));
                }
                
                pos = objEnd + 1;
                if (pos >= json.length() || json.charAt(pos) == ']') break;
                while (pos < json.length() && (json.charAt(pos) == ',' || Character.isWhitespace(json.charAt(pos)))) {
                    pos++;
                }
            }
        } catch (Exception e) {
            System.err.println("Помилка парсингу JSON відгуків: " + e.getMessage());
        }
        
        return new ReviewData(nextId, reviews, reviewItems);
    }

    private static Review parseReview(String json) {
        try {
            long id = extractLong(json, "id");
            long userId = extractLong(json, "userId");
            long productId = extractLong(json, "productId");
            int rating = (int) extractLong(json, "rating");
            String comment = extractString(json, "comment");
            try {
                comment = java.net.URLDecoder.decode(comment, "UTF-8");
            } catch (Exception ignored) {}
            
            Review review = new Review();
            review.setId(id);
            review.setRating(rating);
            review.setComment(comment);
            
            return review;
        } catch (Exception e) {
            System.err.println("Помилка парсингу відгуку: " + e.getMessage());
            return null;
        }
    }
    
    public static class ReviewDataItem {
        public Review review;
        public long userId;
        public long productId;
        
        public ReviewDataItem(Review review, long userId, long productId) {
            this.review = review;
            this.userId = userId;
            this.productId = productId;
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

