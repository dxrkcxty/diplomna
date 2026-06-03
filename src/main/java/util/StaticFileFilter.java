package util;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.*;

public class StaticFileFilter implements HttpHandler {
    private final HttpHandler staticHandler;
    private final HttpHandler apiHandler;
    
    public StaticFileFilter(HttpHandler staticHandler, HttpHandler apiHandler) {
        this.staticHandler = staticHandler;
        this.apiHandler = apiHandler;
    }
    
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        
        if (path.contains(".") && (path.endsWith(".html") || path.endsWith(".css") || 
            path.endsWith(".js") || path.endsWith(".json") || path.endsWith(".png") || 
            path.endsWith(".jpg") || path.endsWith(".jpeg") || path.endsWith(".gif") || 
            path.endsWith(".svg") || path.endsWith(".ico"))) {
            staticHandler.handle(exchange);
        } else {
            apiHandler.handle(exchange);
        }
    }
}

