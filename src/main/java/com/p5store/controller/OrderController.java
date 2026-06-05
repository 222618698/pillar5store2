package com.p5store.controller;

import com.p5store.domain.Order;
import com.p5store.enums.OrderStatus;
import com.p5store.service.OrderService;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    // ── Customer: place order ────────────────────────────────────
    @PostMapping
    public ResponseEntity<Order> placeOrder(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam @NotNull UUID shippingAddressId,
            @RequestParam(required = false) String discountCode) {

        Order order = orderService.placeOrder(
                extractUserId(principal), shippingAddressId, discountCode);
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    // ── Customer: my orders ──────────────────────────────────────
    @GetMapping("/my")
    public ResponseEntity<Page<Order>> myOrders(
            @AuthenticationPrincipal UserDetails principal,
            @PageableDefault(size = 10, sort = "placedAt") Pageable pageable) {
        return ResponseEntity.ok(orderService.findByUser(extractUserId(principal), pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Order> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(orderService.findById(id));
    }

    @GetMapping("/number/{orderNumber}")
    public ResponseEntity<Order> getByOrderNumber(@PathVariable String orderNumber) {
        return ResponseEntity.ok(orderService.findByOrderNumber(orderNumber));
    }

    // ── Customer: cancel ────────────────────────────────────────
    @PostMapping("/{id}/cancel")
    public ResponseEntity<Order> cancel(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(orderService.cancel(id, extractUserId(principal)));
    }

    // ── Admin: all orders ────────────────────────────────────────
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<Order>> allOrders(
            @PageableDefault(size = 20, sort = "placedAt") Pageable pageable) {
        return ResponseEntity.ok(orderService.findAll(pageable));
    }

    // ── Admin: update status ─────────────────────────────────────
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Order> updateStatus(
            @PathVariable UUID id,
            @RequestParam OrderStatus status) {
        return ResponseEntity.ok(orderService.updateStatus(id, status));
    }

    private UUID extractUserId(UserDetails principal) {
        return UUID.fromString(principal.getUsername());
    }
}
