package util;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;

public class DatabaseInitializer {

    public static void initialize() {
        try {
            Connection conn = DatabaseConnection.getConnection();
            String schema = loadSchema();
            String[] statements = schema.split(";");

            try (Statement stmt = conn.createStatement()) {
                for (String statement : statements) {
                    String trimmed = statement.trim();
                    if (!trimmed.isEmpty() && !trimmed.startsWith("--")) {
                        try {
                            stmt.execute(trimmed);
                        } catch (Exception e) {
                            if (!e.getMessage().contains("already exists")) {
                                System.err.println("Помилка виконання SQL: " + trimmed.substring(0, Math.min(50, trimmed.length())) + "...");
                                System.err.println("Деталі: " + e.getMessage());
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Помилка ініціалізації БД: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static String loadSchema() {
        try {
            InputStream inputStream = DatabaseInitializer.class.getClassLoader()
                    .getResourceAsStream("schema.sql");
            if (inputStream == null) {
                inputStream = tryOpenFromFilesystem();
            }
            if (inputStream == null) {
                throw new RuntimeException("Файл schema.sql не знайдено");
            }
            StringBuilder sb = new StringBuilder();
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                sb.append(new String(buffer, 0, bytesRead, StandardCharsets.UTF_8));
            }
            inputStream.close();
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Не вдалося завантажити schema.sql", e);
        }
    }

    private static InputStream tryOpenFromFilesystem() {
        try {
            String userDir = System.getProperty("user.dir");
            Path[] candidates = new Path[] {
                    Paths.get(userDir, "schema.sql"),
                    Paths.get(userDir, "src", "main", "resources", "schema.sql"),
                    Paths.get(userDir, "kursova", "src", "main", "resources", "schema.sql"),
                    Paths.get(userDir, "kursova", "schema.sql")
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
}

