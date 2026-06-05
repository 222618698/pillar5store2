package com.p5store.config;

import com.p5store.domain.*;
import com.p5store.dto.request.ProductRequest;
import com.p5store.dto.response.ProductResponse;
import com.p5store.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Manual mapper — converts between ProductRequest ↔ domain ↔ ProductResponse.
 * Keeps domain entities clean and gives full control over what fields are exposed.
 */
@Component
@RequiredArgsConstructor
public class ProductMapper {

    private final ReviewRepository reviewRepository;

    // ── ProductRequest → Product (domain) ──────────────────────
    public Product toDomain(ProductRequest req, Category category) {
        Product product = Product.builder()
                .category(category)
                .name(req.name())
                .slug(toSlug(req.name()))
                .description(req.description())
                .basePrice(req.basePrice())
                .sku(req.sku().toUpperCase())
                .isActive(req.active())
                .build();

        // variants
        if (req.variants() != null) {
            req.variants().forEach(v -> {
                ProductVariant variant = ProductVariant.builder()
                        .product(product)
                        .size(v.size())
                        .colour(v.colour())
                        .priceModifier(v.priceModifier())
                        .stockQuantity(v.stockQuantity())
                        .imageUrl(v.imageUrl())
                        .build();
                product.getVariants().add(variant);
            });
        }

        // images
        if (req.images() != null) {
            req.images().forEach(img -> {
                ProductImage image = ProductImage.builder()
                        .product(product)
                        .url(img.url())
                        .isPrimary(img.primary())
                        .sortOrder(img.sortOrder())
                        .build();
                product.getImages().add(image);
            });
        }

        return product;
    }

    // ── Product → ProductResponse ───────────────────────────────
    public ProductResponse toResponse(Product p) {
        Double avg = reviewRepository.findAverageRatingByProductId(p.getId()).orElse(null);
        int reviewCount = p.getReviews() != null ? p.getReviews().size() : 0;

        return new ProductResponse(
                p.getId(),
                p.getName(),
                p.getSlug(),
                p.getDescription(),
                p.getSku(),
                p.getBasePrice(),
                p.isActive(),
                new ProductResponse.CategorySummary(
                        p.getCategory().getId(),
                        p.getCategory().getName(),
                        p.getCategory().getSlug()),
                toVariantResponses(p.getVariants()),
                toImageResponses(p.getImages()),
                avg,
                reviewCount,
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }

    public List<ProductResponse> toResponseList(List<Product> products) {
        return products.stream().map(this::toResponse).toList();
    }

    // ── helpers ─────────────────────────────────────────────────
    private List<ProductResponse.VariantResponse> toVariantResponses(List<ProductVariant> variants) {
        if (variants == null) return List.of();
        return variants.stream().map(v -> new ProductResponse.VariantResponse(
                v.getId(),
                v.getSize(),
                v.getColour(),
                v.getPriceModifier(),
                v.getFinalPrice(),
                v.getStockQuantity(),
                v.isInStock(),
                v.getImageUrl()
        )).toList();
    }

    private List<ProductResponse.ImageResponse> toImageResponses(List<ProductImage> images) {
        if (images == null) return List.of();
        return images.stream().map(i -> new ProductResponse.ImageResponse(
                i.getId(),
                i.getUrl(),
                i.isPrimary(),
                i.getSortOrder()
        )).toList();
    }

    private String toSlug(String name) {
        return name.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .strip();
    }
}
