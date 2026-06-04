package com.p5store.service;

import com.p5store.domain.Cart;

import java.util.UUID;

public interface CartService {
    Cart getOrCreateCart(UUID userId);
    Cart addItem(UUID userId, UUID variantId, int quantity);
    Cart updateItem(UUID userId, UUID variantId, int quantity);
    Cart removeItem(UUID userId, UUID variantId);
    void clearCart(UUID userId);
}