package util;

import java.io.InputStream;
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

                InputStream inputStream = DatabaseConnection.class
                        .getClassLoader()
                        .getResourceAsStream(PROPERTIES_FILE);

                if (inputStream == null) {
                    inputStream = tryOpenFromFilesystem();
                }

                if (inputStream == null) {
                    throw new RuntimeException("Файл database.properties не знайдено у resources або файловій системі");
                }

                props.load(inputStream);

                String url = props.getProperty("db.url");
                String username = props.getProperty("db.username");
                String password = props.getProperty("db.password");

                if (url == null || username == null || password == null) {
                    throw new RuntimeException(
                            "Неповні дані у database.properties"
                    );
                }

                connection = DriverManager.getConnection(url, username, password);

            } catch (Exception e) {
                System.err.println("Помилка підключення до БД: " + e.getMessage());
                throw new SQLException("Не вдалося підключитися до БД", e);
            }
        }
        return connection;
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
            System.err.println("Помилка закриття підключення: " + e.getMessage());
        }
    }
}
