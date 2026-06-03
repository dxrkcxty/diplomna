package dto;

public record ProductDTO(long id, String name, double price, long categoryId, 
                        String type, String size, String gender, String imageUrl,
                        Double discountPercent, Double discountAmount) {
    public ProductDTO(long id, String name, double price, long categoryId) {
        this(id, name, price, categoryId, null, null, null, null, null, null);
    }
    
    public ProductDTO(long id, String name, double price, long categoryId, 
                     String type, String size, String gender, String imageUrl) {
        this(id, name, price, categoryId, type, size, gender, imageUrl, null, null);
    }
}
