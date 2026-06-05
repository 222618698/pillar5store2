package com.p5store.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * DTO used by admin when creating or updating a product.
 * Keeps domain entities clean — admin payload never hits the entity directly.
 */
public record ProductRequest(

        @NotNull(message = "Category is required")
        UUID categoryId,

        @NotBlank(message = "Product name is required")
        @Size(max = 200, message = "Name must not exceed 200 characters")
        String name,

        @NotBlank(message = "SKU is required")
        @Size(max = 60, message = "SKU must not exceed 60 characters")
        String sku,

        @Size(max = 5000, message = "Description must not exceed 5000 characters")
        String description,

        @NotNull(message = "Base price is required")
        @DecimalMin(value = "0.01", message = "Base price must be greater than 0")
        BigDecimal basePrice,

        @Valid
        List<VariantRequest> variants,

        List<ImageRequest> images,

        boolean active

) {
    public record VariantRequest(
            String size,
            String colour,

            @NotNull
            @DecimalMin("0.00")
            BigDecimal priceModifier,

            @Min(value = 0, message = "Stock cannot be negative")
            int stockQuantity,

            String imageUrl
    ) {}

    public record ImageRequest(
            @NotBlank String url,
            boolean primary,
            int sortOrder
    ) {}
}
