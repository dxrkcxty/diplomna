package service;

import model.Category;
import model.Product;
import repository.CategoryRepository;
import repository.ProductRepository;
import util.Cache;

import java.util.List;
import java.util.Locale;

public class CategoryService {
    public static final String CAT_APPAREL = "Одяг для футболу";
    public static final String CAT_PLAYER = "Екіпірування гравця";
    public static final String CAT_GOALKEEPER = "Воротарська екіпіровка";
    public static final String CAT_TRAINING = "Тренувальний інвентар";
    public static final String CAT_ACCESSORIES = "Аксесуари";

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final Cache<String, List<Category>> cache = new Cache<>(60000);

    public CategoryService() {
        this.categoryRepository = new CategoryRepository();
        this.productRepository = new ProductRepository();
    }

    public List<Category> getAll() {
        List<Category> cached = cache.get("all");
        if (cached != null) {
            return cached;
        }
        List<Category> all = categoryRepository.findAll();
        cache.put("all", all);
        return all;
    }

    public Category getById(long id) {
        return categoryRepository.findById(id);
    }

    public void ensureDefaultCategories() {
        if (categoryRepository.count() > 0) {
            return;
        }
        categoryRepository.insert(CAT_APPAREL);
        categoryRepository.insert(CAT_PLAYER);
        categoryRepository.insert(CAT_GOALKEEPER);
        categoryRepository.insert(CAT_TRAINING);
        categoryRepository.insert(CAT_ACCESSORIES);
        System.out.println("Створено стартові категорії товарів (5 шт.)");
    }

    public Category resolveByProductType(String type) {
        String t = type == null ? "" : type.toLowerCase(Locale.ROOT).trim();
        if (t.isEmpty()) {
            return categoryRepository.findByName(CAT_ACCESSORIES);
        }
        if (t.contains("бутси") || t.equals("щитки") || t.equals("фіксатор")) {
            return categoryRepository.findByName(CAT_PLAYER);
        }
        if (t.contains("воротар")) {
            return categoryRepository.findByName(CAT_GOALKEEPER);
        }
        if (t.contains("тренувальний інвентар") || t.contains("драбина") || t.contains("конус")) {
            return categoryRepository.findByName(CAT_TRAINING);
        }
        if (t.contains("м'яч") || t.contains("рюкзак") || t.contains("сумка") || t.contains("пляшка")) {
            return categoryRepository.findByName(CAT_ACCESSORIES);
        }
        if (t.contains("футболка") || t.contains("шорти") || t.contains("гетри")
                || t.contains("термобілизна") || t.contains("костюм") || t.contains("куртка")) {
            return categoryRepository.findByName(CAT_APPAREL);
        }
        return categoryRepository.findByName(CAT_ACCESSORIES);
    }

    public int linkProductsToCategories() {
        List<Product> products = productRepository.findAll();
        int updated = 0;
        for (Product product : products) {
            Category category = resolveByProductType(product.getType());
            if (category == null) {
                continue;
            }
            boolean needsUpdate = product.getCategory() == null
                    || product.getCategory().getId() != category.getId();
            if (!needsUpdate) {
                continue;
            }
            product.setCategory(category);
            productRepository.save(product);
            updated++;
        }
        if (updated > 0) {
            System.out.println("Оновлено category_id для " + updated + " товарів");
        }
        return updated;
    }
}
