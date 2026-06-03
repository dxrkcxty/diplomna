package util;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class StaticFileHandler implements HttpHandler {
    private final String rootPath;
    
    public StaticFileHandler(String rootPath) {
        this.rootPath = rootPath;
    }
    
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            setCorsHeaders(exchange);
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
            return;
        }
        
        String path = exchange.getRequestURI().getPath();
        
        if (path.equals("/users") || path.equals("/products") || path.equals("/reviews")) {
            send404(exchange);
            return;
        }
        if ((path.startsWith("/users/") || path.startsWith("/products/") || path.startsWith("/reviews/")) && !path.contains(".")) {
            send404(exchange);
            return;
        }
        
        if (path.equals("/") || path.equals("")) {
            path = "/index.html";
        }
        
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        
        Path filePath = Paths.get(rootPath, path);
        File file = filePath.toFile();
        
        if (!file.exists() || !file.isFile() || !file.getCanonicalPath().startsWith(new File(rootPath).getCanonicalPath())) {
            send404(exchange);
            return;
        }
        
        String contentType = getContentType(file.getName());
        
        byte[] fileBytes = Files.readAllBytes(filePath);
        
        setCorsHeaders(exchange);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(200, fileBytes.length);
        
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(fileBytes);
        }
    }
    
    private void setCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }
    
    private String getContentType(String fileName) {
        String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
        return switch (extension) {
            case "html" -> "text/html; charset=UTF-8";
            case "css" -> "text/css; charset=UTF-8";
            case "js" -> "application/javascript; charset=UTF-8";
            case "json" -> "application/json; charset=UTF-8";
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            case "gif" -> "image/gif";
            case "svg" -> "image/svg+xml";
            case "ico" -> "image/x-icon";
            default -> "application/octet-stream";
        };
    }
    
    private void send404(HttpExchange exchange) throws IOException {
        String response = "404 Not Found";
        exchange.sendResponseHeaders(404, response.length());
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes());
        }
    }
}

