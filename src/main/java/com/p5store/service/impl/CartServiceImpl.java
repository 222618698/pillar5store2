package com.p5store.service.impl;

import com.p5store.domain.*;
import com.p5store.exception.BadRequestException;
import com.p5store.exception.ResourceNotFoundException;
import com.p5store.repository.CartRepository;
import com.p5store.repository.ProductVariantRepository;
import com.p5store.repository.UserRepository;
import com.p5store.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final UserRepository userRepository;
    private final ProductVariantRepository variantRepository;

    @Override
    public Cart getOrCreateCart(UUID userId) {
        return cartRepository.findByUserIdWithItems(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new ResourceNotFoundException("User", userId));
                    Cart cart = Cart.builder().user(user).build();
                    return cartRepository.save(cart);
                });
    }

    @Override
    public Cart addItem(UUID userId, UUID variantId, int quantity) {
        if (quantity <= 0) throw new BadRequestException("Quantity must be at least 1");

        Cart cart = getOrCreateCart(userId);
        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("ProductVariant", variantId));

        if (!variant.isInStock()) {
            throw new BadRequestException("Product variant is out of stock");
        }
        if (variant.getStockQuantity() < quantity) {
            throw new BadRequestException("Only " + variant.getStockQuantity() + " units available");
        }

        // If item already exists in cart, increment quantity
        cart.getItems().stream()
                .filter(i -> i.getProductVariant().getId().equals(variantId))
                .findFirst()
                .ifPresentOrElse(
                        existing -> existing.setQuantity(existing.getQuantity() + quantity),
                        () -> cart.getItems().add(
                                CartItem.builder()
                                        .cart(cart)
                                        .productVariant(variant)
                                        .quantity(quantity)
                                        .build())
                );
        return cartRepository.save(cart);
    }

    @Override
    public Cart updateItem(UUID userId, UUID variantId, int quantity) {
        Cart cart = getOrCreateCart(userId);

        if (quantity <= 0) {
            return removeItem(userId, variantId);
        }

        CartItem item = cart.getItems().stream()
                .filter(i -> i.getProductVariant().getId().equals(variantId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found for variant " + variantId));

        ProductVariant variant = item.getProductVariant();
        if (variant.getStockQuantity() < quantity) {
            throw new BadRequestException("Only " + variant.getStockQuantity() + " units available");
        }
        item.setQuantity(quantity);
        return cartRepository.save(cart);
    }

    @Override
    public Cart removeItem(UUID userId, UUID variantId) {
        Cart cart = getOrCreateCart(userId);
        cart.getItems().removeIf(i -> i.getProductVariant().getId().equals(variantId));
        return cartRepository.save(cart);
    }

    @Override
    public void clearCart(UUID userId) {
        Cart cart = getOrCreateCart(userId);
        cart.clear();
        cartRepository.save(cart);
    }
}
