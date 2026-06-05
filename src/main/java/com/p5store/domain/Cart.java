package com.p5store.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "carts")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cart extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<CartItem> items = new ArrayList<>();

    // ── Helpers ────────────────────────────────────────────
    public int getTotalItems() {
        return items.stream().mapToInt(CartItem::getQuantity).sum();
    }

    public BigDecimal getSubtotal() {
        return items.stream()
                .map(CartItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void clear() {
        items.clear();
    }
}
