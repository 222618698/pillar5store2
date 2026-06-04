package com.p5store.service.impl;

import com.p5store.domain.*;
import com.p5store.enums.OrderStatus;
import com.p5store.exception.*;
import com.p5store.repository.*;
import com.p5store.service.CartService;
import com.p5store.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final CartRepository cartRepository;
    private final DiscountRepository discountRepository;
    private final CartService cartService;

    // ── Place Order ─────────────────────────────────────────────
    @Override
    public Order placeOrder(UUID userId, UUID shippingAddressId, String discountCode) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        Address address = addressRepository.findById(shippingAddressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address", shippingAddressId));

        // Verify address belongs to user
        if (!address.getUser().getId().equals(userId)) {
            throw new BadRequestException("Address does not belong to this user");
        }

        Cart cart = cartRepository.findByUserIdWithItems(userId)
                .orElseThrow(() -> new BadRequestException("Cart is empty"));

        if (cart.getItems().isEmpty()) {
            throw new BadRequestException("Cannot place order with empty cart");
        }

        // ── Validate stock & deduct ──────────────────────────────
        for (CartItem item : cart.getItems()) {
            ProductVariant variant = item.getProductVariant();
            if (variant.getStockQuantity() < item.getQuantity()) {
                throw new InsufficientStockException(
                        variant.getId().toString(),
                        variant.getStockQuantity(),
                        item.getQuantity());
            }
            variant.deductStock(item.getQuantity());
        }

        BigDecimal subtotal = cart.getSubtotal();
        BigDecimal shipping = calculateShipping(subtotal);
        BigDecimal discountAmount = BigDecimal.ZERO;
        Discount discount = null;

        // ── Apply discount code ──────────────────────────────────
        if (discountCode != null && !discountCode.isBlank()) {
            discount = discountRepository.findByCodeIgnoreCase(discountCode)
                    .orElseThrow(() -> new InvalidDiscountException(discountCode));
            if (!discount.isValid(subtotal)) {
                throw new InvalidDiscountException(discountCode);
            }
            discountAmount = discount.calculateDiscount(subtotal);
            discount.incrementUsage();
        }

        // ── Build Order ──────────────────────────────────────────
        Order order = Order.builder()
                .user(user)
                .shippingAddress(address)
                .orderNumber(generateOrderNumber())
                .status(OrderStatus.PENDING)
                .subtotal(subtotal)
                .shippingCost(shipping)
                .discountAmount(discountAmount)
                .total(subtotal.add(shipping).subtract(discountAmount))
                .placedAt(LocalDateTime.now())
                .build();

        // ── Build Order Items (snapshot prices) ──────────────────
        for (CartItem cartItem : cart.getItems()) {
            ProductVariant variant = cartItem.getProductVariant();
            BigDecimal unitPrice = variant.getFinalPrice();
            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .productVariant(variant)
                    .quantity(cartItem.getQuantity())
                    .unitPrice(unitPrice)
                    .lineTotal(unitPrice.multiply(BigDecimal.valueOf(cartItem.getQuantity())))
                    .build();
            order.getItems().add(orderItem);
        }

        // ── Attach discount record ───────────────────────────────
        if (discount != null) {
            OrderDiscount od = OrderDiscount.builder()
                    .order(order)
                    .discount(discount)
                    .appliedAmount(discountAmount)
                    .build();
            order.getDiscounts().add(od);
        }

        Order saved = orderRepository.save(order);

        // ── Clear cart ───────────────────────────────────────────
        cart.clear();
        cartRepository.save(cart);

        log.info("Order {} placed for user {} — total R{}", saved.getOrderNumber(), userId, saved.getTotal());
        return saved;
    }

    // ── Queries ─────────────────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public Order findById(UUID id) {
        return orderRepository.findByIdWithItems(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", id));
    }

    @Override
    @Transactional(readOnly = true)
    public Order findByOrderNumber(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderNumber));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Order> findByUser(UUID userId, Pageable pageable) {
        return orderRepository.findByUserId(userId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Order> findAll(Pageable pageable) {
        return orderRepository.findAll(pageable);
    }

    // ── Status update (admin) ────────────────────────────────────
    @Override
    public Order updateStatus(UUID id, OrderStatus newStatus) {
        Order order = findById(id);
        order.setStatus(newStatus);
        return orderRepository.save(order);
    }

    // ── Cancel ──────────────────────────────────────────────────
    @Override
    public Order cancel(UUID id, UUID requestingUserId) {
        Order order = findById(id);

        boolean isOwner = order.getUser().getId().equals(requestingUserId);
        if (!isOwner) throw new BadRequestException("Cannot cancel another user's order");
        if (!order.isCancellable()) {
            throw new BadRequestException("Order " + order.getOrderNumber() + " cannot be cancelled — status: " + order.getStatus());
        }

        // Return stock
        order.getItems().forEach(item ->
                item.getProductVariant().returnStock(item.getQuantity()));

        order.setStatus(OrderStatus.CANCELLED);
        return orderRepository.save(order);
    }

    // ── Helpers ──────────────────────────────────────────────────
    private String generateOrderNumber() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int rand = ThreadLocalRandom.current().nextInt(1000, 9999);
        return "P5-" + date + "-" + rand;
    }

    private BigDecimal calculateShipping(BigDecimal subtotal) {
        // Free shipping over R500
        return subtotal.compareTo(new BigDecimal("500.00")) >= 0
                ? BigDecimal.ZERO
                : new BigDecimal("79.00");
    }
}