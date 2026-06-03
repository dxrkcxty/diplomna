package util;

import java.io.InputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseConnection {

    private static Connection connection;
    private static final String PROPERTIES_FILE = "database.properties";

    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            try {
                Properties props = new Properties();
                String databaseUrl = System.getenv("DATABASE_URL");

                if (databaseUrl != null && !databaseUrl.isBlank()) {
                    applyRenderDatabaseUrl(props, databaseUrl);
                } else {
                    try (InputStream inputStream = openProperties()) {
                        if (inputStream == null) {
                            throw new RuntimeException("database.properties not found");
                        }
                        props.load(inputStream);
                    }
                }

                String url = props.getProperty("db.url");
                String username = props.getProperty("db.username");
                String password = props.getProperty("db.password");

                if (url == null || username == null || password == null) {
                    throw new RuntimeException("Incomplete database configuration");
                }

                connection = DriverManager.getConnection(url, username, password);

            } catch (Exception e) {
                System.err.println("Database connection error: " + e.getMessage());
                throw new SQLException("Could not connect to database", e);
            }
        }
        return connection;
    }

    private static void applyRenderDatabaseUrl(Properties props, String databaseUrl) {
        URI uri = URI.create(databaseUrl);
        String userInfo = uri.getUserInfo();
        String username = "";
        String password = "";
        if (userInfo != null) {
            String[] parts = userInfo.split(":", 2);
            username = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
            if (parts.length > 1) {
                password = URLDecoder.decode(parts[1], StandardCharsets.UTF_8);
            }
        }

        String jdbcUrl = "jdbc:postgresql://" + uri.getHost();
        if (uri.getPort() != -1) {
            jdbcUrl += ":" + uri.getPort();
        }
        jdbcUrl += uri.getPath();

        props.setProperty("db.url", jdbcUrl);
        props.setProperty("db.username", username);
        props.setProperty("db.password", password);
    }

    private static InputStream openProperties() {
        InputStream resourceStream = DatabaseConnection.class
                .getClassLoader()
                .getResourceAsStream(PROPERTIES_FILE);
        if (resourceStream != null) {
            return resourceStream;
        }
        return tryOpenFromFilesystem();
    }

    private static InputStream tryOpenFromFilesystem() {
        try {
            String userDir = System.getProperty("user.dir");
            Path[] candidates = new Path[] {
                    Paths.get(userDir, "database.properties"),
                    Paths.get(userDir, "src", "main", "resources", "database.properties"),
                    Paths.get(userDir, "kursova", "src", "main", "resources", "database.properties"),
                    Paths.get(userDir, "kursova", "database.properties")
            };
            for (Path p : candidates) {
                if (Files.isRegularFile(p)) {
                    return Files.newInputStream(p);
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    public static void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            System.err.println("Database close error: " + e.getMessage());
        }
    }
}
