package service;

import model.Product;
import model.Category;
import model.Review;
import repository.ProductRepository;
import util.Cache;
import java.util.*;
import java.util.stream.Collectors;

public class ProductService {
    private final ProductRepository productRepository;
    private Cache<String, List<Product>> cache = new Cache<>(300000);

    public ProductService() {
        this.productRepository = new ProductRepository();
    }

    public List<Product> getAll() {
        List<Product> cached = cache.get("all");
        if (cached != null) return cached;
        List<Product> all = productRepository.findAll();
        cache.put("all", all);
        return all;
    }
    
    public Product getById(long id) {
        return productRepository.findById(id);
    }
    
    public void clearCache() {
        cache.clear();
    }

    public Product create(Product product) {
        cache.clear();
        Product created = productRepository.save(product);
        return created;
    }
    
    public Product update(long id, Product newProduct) {
        Product existing = productRepository.findById(id);
        if (existing == null) return null;
        newProduct.setId(id);
        cache.clear();
        Product updated = productRepository.save(newProduct);
        return updated;
    }
    
    public boolean delete(long id) {
        boolean result = productRepository.deleteById(id);
        if(result) {
            cache.clear();
        }
        return result;
    }
    public List<Product> filterByCategory(long categoryId) {
        String key = "cat-" + categoryId;
        List<Product> cached = cache.get(key);
        if (cached != null) return cached;
        List<Product> res = productRepository.findByCategoryId(categoryId);
        cache.put(key, res);
        return res;
    }
    
    public List<Product> find(Long categoryId, Double minPrice, Double maxPrice, String name, String sort, String order, int offset, int limit) {
        List<Product> result = productRepository.findAll();
        if (categoryId != null) result = result.stream().filter(p -> p.getCategory() != null && p.getCategory().getId() == categoryId).collect(Collectors.toList());
        if (minPrice != null) result = result.stream().filter(p -> p.getPrice() >= minPrice).collect(Collectors.toList());
        if (maxPrice != null) result = result.stream().filter(p -> p.getPrice() <= maxPrice).collect(Collectors.toList());
        if (name != null && !name.isBlank()) result = result.stream().filter(p -> p.getName() != null && p.getName().toLowerCase().contains(name.toLowerCase())).collect(Collectors.toList());
     
        Comparator<Product> comparator = null;
        if (sort != null) {
            switch(sort) {
                case "price": comparator = Comparator.comparing(Product::getPrice); break;
                case "name": comparator = Comparator.comparing(Product::getName, Comparator.nullsLast(String::compareToIgnoreCase)); break;
                case "id": comparator = Comparator.comparing(Product::getId); break;
            }
        }
        if (comparator != null) {
            if("desc".equalsIgnoreCase(order)) comparator = comparator.reversed();
            result = result.stream().sorted(comparator).collect(Collectors.toList());
        }
    
        int from = Math.max(0, offset);
        int to = limit > 0 ? Math.min(result.size(), from + limit) : result.size();
        if (from > to) from = to;
        return result.subList(from, to);
    }
    
    public List<Product> getDiscountedProducts() {
        List<Product> cached = cache.get("discounted");
        if (cached != null) {
            return cached;
        }
        List<Product> discounted = productRepository.findWithDiscounts();
        cache.put("discounted", discounted);
        return discounted;
    }

    public List<Product> getOrderedByUserId(long userId) {
        if (userId <= 0) {
            return List.of();
        }
        return productRepository.findOrderedByUserId(userId);
    }

    public List<Product> getFiltered(String name, Double minPrice, Double maxPrice, Long categoryId, String sortBy, String sortOrder, Integer offset, Integer limit) {
        List<Product> all = productRepository.findAll();

        if (name != null && !name.isEmpty()) {
            all = all.stream().filter(p -> p.getName() != null && p.getName().toLowerCase().contains(name.toLowerCase())).collect(Collectors.toList());
        }
        if (minPrice != null) {
            all = all.stream().filter(p -> p.getPrice() >= minPrice).collect(Collectors.toList());
        }
        if (maxPrice != null) {
            all = all.stream().filter(p -> p.getPrice() <= maxPrice).collect(Collectors.toList());
        }
        if (categoryId != null && categoryId > 0) {
            all = all.stream().filter(p -> p.getCategory() != null && p.getCategory().getId() == categoryId).collect(Collectors.toList());
        }
    
        if (sortBy != null) {
            Comparator<Product> comparator = null;
            if (sortBy.equals("name")) comparator = Comparator.comparing(Product::getName, Comparator.nullsLast(String::compareToIgnoreCase));
            else if (sortBy.equals("price")) comparator = Comparator.comparing(Product::getPrice);
            if (comparator != null) {
                if (sortOrder != null && sortOrder.equalsIgnoreCase("desc")) {
                    comparator = comparator.reversed();
                }
                all = all.stream().sorted(comparator).collect(Collectors.toList());
            }
        }
   
        if (offset == null && limit == null) {
            return all;
        }
        if (offset == null || offset < 0) offset = 0;
        if (limit == null || limit <= 0) limit = all.size();
        int fromIdx = Math.min(offset, all.size());
        int toIdx = Math.min(fromIdx + limit, all.size());
        if (fromIdx >= toIdx) {
            return new ArrayList<>();
        }
        List<Product> result = all.subList(fromIdx, toIdx);
        return result;
    }
}
