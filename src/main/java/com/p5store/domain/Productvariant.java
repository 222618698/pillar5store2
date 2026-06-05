package com.p5store.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "product_variants")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductVariant extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(length = 50)
    private String size;

    @Column(length = 50)
    private String colour;

    /**
     * Price modifier added to (or subtracted from) the product's base_price.
     * A value of 0 means the variant costs the same as the base.
     */
    @Column(name = "price_modifier", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal priceModifier = BigDecimal.ZERO;

    @Column(name = "stock_quantity", nullable = false)
    @Builder.Default
    private int stockQuantity = 0;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    // ── Helpers ────────────────────────────────────────────
    public BigDecimal getFinalPrice() {
        return product.getBasePrice().add(priceModifier);
    }

    public boolean isInStock() {
        return stockQuantity > 0;
    }

    /**
     * Reduce stock. Throws if insufficient.
     */
    public void deductStock(int quantity) {
        if (this.stockQuantity < quantity) {
            throw new IllegalStateException(
                "Insufficient stock for variant " + getId() +
                ". Available: " + this.stockQuantity + ", requested: " + quantity);
        }
        this.stockQuantity -= quantity;
    }

    /**
     * Return stock (e.g. on order cancellation).
     */
    public void returnStock(int quantity) {
        this.stockQuantity += quantity;
    }
}
