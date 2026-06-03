package util;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class AppProperties {
    private static final String PROPERTIES_FILE = "database.properties";

    public static Properties load() {
        Properties props = new Properties();
        try {
            InputStream inputStream = tryOpenFromFilesystem();
            if (inputStream == null) {
                inputStream = AppProperties.class
                        .getClassLoader()
                        .getResourceAsStream(PROPERTIES_FILE);
            }
            if (inputStream == null) {
                return props;
            }
            props.load(inputStream);
            try { inputStream.close(); } catch (Exception ignored) {}
        } catch (Exception ignored) {
        }
        return props;
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
}

