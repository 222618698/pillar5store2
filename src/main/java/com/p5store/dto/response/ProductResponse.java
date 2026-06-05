package com.p5store.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Full product view returned to both customers and admin.
 * Includes category name, variants with stock/pricing, images, and review summary.
 */
public record ProductResponse(

        UUID id,
        String name,
        String slug,
        String description,
        String sku,
        BigDecimal basePrice,
        boolean active,

        CategorySummary category,
        List<VariantResponse> variants,
        List<ImageResponse> images,

        Double averageRating,
        int reviewCount,

        LocalDateTime createdAt,
        LocalDateTime updatedAt

) {

    public record CategorySummary(UUID id, String name, String slug) {}

    public record VariantResponse(
            UUID id,
            String size,
            String colour,
            BigDecimal priceModifier,
            BigDecimal finalPrice,
            int stockQuantity,
            boolean inStock,
            String imageUrl
    ) {}

    public record ImageResponse(
            UUID id,
            String url,
            boolean primary,
            int sortOrder
    ) {}
}
