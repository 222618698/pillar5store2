package com.p5store.repository;

import com.p5store.domain.Order;
import com.p5store.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    Page<Order> findByUserId(UUID userId, Pageable pageable);

    Optional<Order> findByOrderNumber(String orderNumber);

    Page<Order> findByStatus(OrderStatus status, Pageable pageable);

    @Query("SELECT o FROM Order o JOIN FETCH o.items i JOIN FETCH i.productVariant WHERE o.id = :id")
    Optional<Order> findByIdWithItems(@Param("id") UUID id);
}