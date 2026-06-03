package dto;

public record ReviewDTO(long id, long userId, long productId, int rating, String comment) {}
