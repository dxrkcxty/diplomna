package dto;

public record UserDTO(
        long id,
        String email,
        String password,
        String role,
        String firstName,
        String lastName,
        String phone,
        double bonusBalance,
        Integer bonusRate,
        int spinCredits
) {}
