package com.p5store.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "wishlist_items",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "product_variant_id"}))
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WishlistItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_variant_id", nullable = false)
    private ProductVariant productVariant;

    @Column(name = "added_at", nullable = false)
    @Builder.Default
    private LocalDateTime addedAt = LocalDateTime.now();
}
