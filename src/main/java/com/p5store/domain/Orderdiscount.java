package com.p5store.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "order_discounts")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderDiscount extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "discount_id", nullable = false)
    private Discount discount;

    @Column(name = "applied_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal appliedAmount;
}
