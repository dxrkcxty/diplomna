import com.sun.net.httpserver.HttpServer;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.SpringApplication;
import com.kursova.BootstrapConfig;

import controller.UserController;
import controller.ProductController;
import controller.ReviewController;
import controller.OrderController;
import controller.CategoryController;

import service.UserService;
import service.ProductService;
import service.OrderService;
import service.ReviewService;
import service.CategoryService;

import util.StaticFileHandler;
import util.StaticFileFilter;
import util.DatabaseInitializer;
import util.DatabaseConnection;

import model.User;
import model.Product;
import model.Category;

import java.net.InetSocketAddress;
import java.io.File;
import java.sql.Connection;
import java.util.List;

public class Application {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(BootstrapConfig.class);
        application.setWebApplicationType(WebApplicationType.NONE);
        application.run(args);

        try {
            new Application().start();
        } catch (Exception e) {
            throw new RuntimeException("Помилка запуску застосунку", e);
        }
    }

    private void start() throws Exception {
        DatabaseInitializer.initialize();

        try (Connection connection = DatabaseConnection.getConnection()) {
            System.out.println("Підключення до PostgreSQL успішне");
        } catch (Exception e) {
            System.out.println("КРИТИЧНА ПОМИЛКА: не вдалося підключитися до БД");
            e.printStackTrace();
            return;
        }


        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        UserService userService = new UserService();
        ProductService productService = new ProductService();
        CategoryService categoryService = new CategoryService();

        categoryService.ensureDefaultCategories();
        categoryService.linkProductsToCategories();
        productService.clearCache();

        try {
            if (!userService.existsByEmail("admin@gmail.com")) {
                User admin = new User(0, "admin@gmail.com", "admin123", "ADMIN", List.of());
                userService.create(admin);
                System.out.println("Тестовий адміністратор створено: admin@gmail.com / admin123");
            }

            if (!userService.existsByEmail("user@gmail.com")) {
                User user = new User(0, "user@gmail.com", "user123", "USER", List.of());
                userService.create(user);
                System.out.println("Тестовий користувач створено: user@gmail.com / user123");
            }

            System.out.println("Користувачі завантажені з бази даних");

        } catch (Exception e) {
            System.out.println("Примітка: помилка створення тестових користувачів: " + e.getMessage());
        }


        try {
            List<Product> existingProducts = productService.getAll();
            if (!existingProducts.isEmpty()) {
                System.out.println("Каталог вже заповнений, стартовий посів пропущено");
            } else {
                System.out.println("Каталог порожній: виконуємо стартовий посів товарів");

                productService.create(catalogProduct(categoryService, "Nike Mercurial Vapor 15 Elite FG", 4999.99,
                    "бутси", "42", "унісекс", "/assets/images/product-default.svg"));
                productService.create(catalogProduct(categoryService, "Adidas Predator Accuracy.1", 4699.99,
                    "бутси", "43", "чоловічий", "/assets/images/product-default.svg"));
                productService.create(catalogProduct(categoryService, "Puma Future Ultimate FG", 4499.99,
                    "бутси", "41", "унісекс", "/assets/images/product-default.svg"));
                productService.create(catalogProduct(categoryService, "Ігрова футболка Team Pro Home", 1399.99,
                    "ігрова футболка", "L", "унісекс", "/assets/images/product-default.svg"));
                productService.create(catalogProduct(categoryService, "Ігрові шорти Matchday Black", 799.99,
                    "ігрові шорти", "M", "чоловічий", "/assets/images/product-default.svg"));
                productService.create(catalogProduct(categoryService, "Гетри Compression Pro", 349.99,
                    "гетри", "L", "унісекс", "/assets/images/product-default.svg"));
                productService.create(catalogProduct(categoryService, "Термобілизна Winter Training", 1149.99,
                    "термобілизна", "M", "унісекс", "/assets/images/product-default.svg"));
                productService.create(catalogProduct(categoryService, "Щитки Shield Carbon", 649.99,
                    "щитки", "M", "унісекс", "/assets/images/product-default.svg"));
                productService.create(catalogProduct(categoryService, "Фіксатор гомілкостопа Active Support", 499.99,
                    "фіксатор", "L", "унісекс", "/assets/images/product-default.svg"));
                productService.create(catalogProduct(categoryService, "Спортивна пляшка 0.75L Team", 249.99,
                    "пляшка", "one size", "унісекс", "/assets/images/product-default.svg"));
                productService.create(catalogProduct(categoryService, "Воротарські рукавиці Grip Ultra", 1299.99,
                    "воротарські рукавиці", "9", "унісекс", "/assets/images/product-default.svg"));
                productService.create(catalogProduct(categoryService, "Воротарський світшот Guard Pro", 1599.99,
                    "воротарський світшот", "XL", "чоловічий", "/assets/images/product-default.svg"));
                productService.create(catalogProduct(categoryService, "Воротарські штани Protect", 1349.99,
                    "воротарські штани", "L", "чоловічий", "/assets/images/product-default.svg"));
                productService.create(catalogProduct(categoryService, "Тренувальний костюм Team Line", 1799.99,
                    "тренувальний костюм", "XL", "унісекс", "/assets/images/product-default.svg"));
                productService.create(catalogProduct(categoryService, "Куртка для тренувань Rain Shield", 1999.99,
                    "куртка для тренувань", "L", "унісекс", "/assets/images/product-default.svg"));
                productService.create(catalogProduct(categoryService, "Координаційна драбина 6м", 929.99,
                    "тренувальний інвентар", "one size", "унісекс", "/assets/images/product-default.svg"));
                productService.create(catalogProduct(categoryService, "Набір конусів для тренувань", 649.99,
                    "тренувальний інвентар", "one size", "унісекс", "/assets/images/product-default.svg"));
                productService.create(catalogProduct(categoryService, "Футбольний м'яч Match Pro", 949.99,
                    "м'яч", "5", "унісекс", "/assets/images/product-default.svg"));
                productService.create(catalogProduct(categoryService, "Рюкзак Team Gear 35L", 1099.99,
                    "рюкзак", "35L", "унісекс", "/assets/images/product-default.svg"));
                productService.create(catalogProduct(categoryService, "Сумка для бутс Boot Bag", 579.99,
                    "сумка", "one size", "унісекс", "/assets/images/product-default.svg"));

                System.out.println("Стартовий каталог створено");
            }

        } catch (Exception e) {
            System.out.println("Примітка: помилка створення тестових товарів: " + e.getMessage());
        }


        File rootDir = resolveStaticRoot();

        StaticFileHandler staticHandler = new StaticFileHandler(rootDir.getAbsolutePath());

        UserController userController = new UserController(userService);
        ProductController productController = new ProductController(productService);

        OrderService orderService = new OrderService(userService, productService);
        OrderController orderController = new OrderController(orderService, userService);

        ReviewService reviewService = new ReviewService(userService, productService);
        ReviewController reviewController = new ReviewController(
                reviewService, userService, productService, orderService
        );
        CategoryController categoryController = new CategoryController(categoryService);

        server.createContext("/api/users", userController);
        server.createContext("/api/categories", categoryController);
        server.createContext("/api/products", new StaticFileFilter(staticHandler, productController));
        server.createContext("/api/orders", orderController);
        server.createContext("/api/reviews", reviewController);

        server.createContext("/", staticHandler);

        server.setExecutor(null);

        server.start();
        System.out.println("Server started on port " + port);
    }

    private File resolveStaticRoot() {
        java.util.ArrayList<File> candidates = new java.util.ArrayList<>();
        String currentDir = System.getProperty("user.dir");
        candidates.add(new File(currentDir));
        candidates.add(new File(currentDir, "kursova"));

        try {
            java.net.URL codeLocation = Application.class.getProtectionDomain().getCodeSource().getLocation();
            if (codeLocation != null) {
                File classesDir = new File(codeLocation.toURI());
                File moduleRoot = classesDir.getParentFile();
                if (moduleRoot != null) {
                    File fromTarget = moduleRoot.getParentFile();
                    if (fromTarget != null) {
                        candidates.add(fromTarget);
                    }
                }
            }
        } catch (Exception ignored) {
        }

        for (File dir : candidates) {
            if (dir != null && new File(dir, "index.html").isFile()) {
                return dir;
            }
        }
        return new File(currentDir);
    }

    private static Product catalogProduct(CategoryService categoryService, String name, double price,
                                          String type, String size, String gender, String imageUrl) {
        Category category = categoryService.resolveByProductType(type);
        return new Product(0, name, price, category, List.of(), type, size, gender, imageUrl);
    }
}
