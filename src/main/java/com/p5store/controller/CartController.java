package com.p5store.controller;

import com.p5store.domain.Cart;
import com.p5store.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ResponseEntity<Cart> getCart(@AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(cartService.getOrCreateCart(extractUserId(principal)));
    }

    @PostMapping("/items")
    public ResponseEntity<Cart> addItem(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam UUID variantId,
            @RequestParam(defaultValue = "1") int quantity) {
        return ResponseEntity.ok(cartService.addItem(extractUserId(principal), variantId, quantity));
    }

    @PutMapping("/items/{variantId}")
    public ResponseEntity<Cart> updateItem(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable UUID variantId,
            @RequestParam int quantity) {
        return ResponseEntity.ok(cartService.updateItem(extractUserId(principal), variantId, quantity));
    }

    @DeleteMapping("/items/{variantId}")
    public ResponseEntity<Cart> removeItem(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable UUID variantId) {
        return ResponseEntity.ok(cartService.removeItem(extractUserId(principal), variantId));
    }

    @DeleteMapping
    public ResponseEntity<Void> clearCart(@AuthenticationPrincipal UserDetails principal) {
        cartService.clearCart(extractUserId(principal));
        return ResponseEntity.noContent().build();
    }

    private UUID extractUserId(UserDetails principal) {
        return UUID.fromString(principal.getUsername());
    }
}
