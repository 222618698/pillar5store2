package com.p5store.domain;

import com.p5store.enums.DiscountType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "discounts")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Discount extends BaseEntity {

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DiscountType type;

    /** Percentage (0–100) or flat monetary amount depending on type. */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal value;

    @Column(name = "min_order_value", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal minOrderValue = BigDecimal.ZERO;

    @Column(name = "usage_limit")
    private Integer usageLimit;

    @Column(name = "usage_count", nullable = false)
    @Builder.Default
    private int usageCount = 0;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    // ── Helpers ────────────────────────────────────────────
    public boolean isValid(BigDecimal orderSubtotal) {
        if (!isActive) return false;
        if (expiresAt != null && LocalDateTime.now().isAfter(expiresAt)) return false;
        if (usageLimit != null && usageCount >= usageLimit) return false;
        return orderSubtotal.compareTo(minOrderValue) >= 0;
    }

    public BigDecimal calculateDiscount(BigDecimal subtotal) {
        return switch (type) {
            case PERCENTAGE -> subtotal.multiply(value).divide(BigDecimal.valueOf(100));
            case FLAT       -> value.min(subtotal);   // never discount more than subtotal
        };
    }

    public void incrementUsage() {
        this.usageCount++;
    }
}
