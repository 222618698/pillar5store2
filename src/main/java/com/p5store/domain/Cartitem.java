package com.p5store.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "cart_items",
       uniqueConstraints = @UniqueConstraint(columnNames = {"cart_id", "product_variant_id"}))
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_variant_id", nullable = false)
    private ProductVariant productVariant;

    @Column(nullable = false)
    private int quantity;

    // ── Helpers ────────────────────────────────────────────
    public BigDecimal getLineTotal() {
        return productVariant.getFinalPrice()
                .multiply(BigDecimal.valueOf(quantity));
    }
}
