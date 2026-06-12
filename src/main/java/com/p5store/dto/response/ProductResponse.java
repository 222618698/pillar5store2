package com.p5store.dto.response;

import java.math.BigDecimal;

public record ProductResponse(
    Long id,
    String name,
    String description,
    String sku,
    BigDecimal price,
    BigDecimal compareAtPrice,
    int stockQuantity,
    String imageUrl,
    String badge,
    boolean featured,
    String status,
    String categoryName,
    Long categoryId
) {}
