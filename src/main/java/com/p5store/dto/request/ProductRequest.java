package com.p5store.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record ProductRequest(
    @NotBlank String name,
    String description,
    @NotBlank String sku,
    @NotNull @DecimalMin("0.01") BigDecimal price,
    BigDecimal compareAtPrice,
    @Min(0) int stockQuantity,
    String imageUrl,
    String badge,
    boolean featured,
    @NotNull Long categoryId
) {}
