package service;

import model.Review;
import repository.ReviewRepository;
import java.util.List;

public class ReviewService {
    private final ReviewRepository reviewRepository;

    public ReviewService(UserService userService, ProductService productService) {
        this.reviewRepository = new ReviewRepository();
    }

    public List<Review> getAll() {
        return reviewRepository.findAll();
    }
    
    public List<Review> getByProductId(long productId) {
        return reviewRepository.findByProductId(productId);
    }
    
    public Review getById(long id) {
        return reviewRepository.findById(id);
    }
    
    public Review create(Review review) {
        Review created = reviewRepository.save(review);
        return created;
    }
    
    public Review update(long id, Review review) {
        Review existing = reviewRepository.findById(id);
        if (existing == null) return null;
        review.setId(id);
        Review updated = reviewRepository.save(review);
        return updated;
    }
    
    public boolean delete(long id) {
        return reviewRepository.deleteById(id);
    }
}
