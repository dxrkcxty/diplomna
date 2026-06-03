package dto;

import java.util.List;
public record OrderDTO(
        long id,
        long userId,
        List<Long> productIds,
        String date,
        String status,
        String fullName,
        String phone,
        String deliveryAddress,
        String deliveryCarrier,
        String deliveryBranch,
        String paymentMethod,
        String paymentStatus,
        double totalAmount,
        double bonusUsed,
        double bonusEarned,
        Integer bonusRate,
        String itemsSnapshot
) {}
