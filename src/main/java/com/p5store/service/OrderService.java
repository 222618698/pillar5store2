package com.p5store.service;

import com.p5store.domain.Order;
import com.p5store.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface OrderService {
    /** Converts the user's current cart into an order. */
    Order placeOrder(UUID userId, UUID shippingAddressId, String discountCode);

    Order findById(UUID id);
    Order findByOrderNumber(String orderNumber);

    Page<Order> findByUser(UUID userId, Pageable pageable);
    Page<Order> findAll(Pageable pageable);

    Order updateStatus(UUID id, OrderStatus newStatus);
    Order cancel(UUID id, UUID requestingUserId);
}