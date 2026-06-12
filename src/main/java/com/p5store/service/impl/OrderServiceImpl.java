package com.p5store.service.impl;

import com.p5store.domain.*;
import com.p5store.dto.request.PlaceOrderRequest;
import com.p5store.dto.response.OrderResponse;
import com.p5store.exception.BusinessException;
import com.p5store.exception.ResourceNotFoundException;
import com.p5store.repository.AddressRepository;
import com.p5store.repository.CartRepository;
import com.p5store.repository.OrderRepository;
import com.p5store.repository.ProductRepository;
import com.p5store.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private static final BigDecimal FREE_SHIPPING_THRESHOLD = new BigDecimal("500.00");
    private static final BigDecimal SHIPPING_COST = new BigDecimal("50.00");

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final AddressRepository addressRepository;
    private final ProductRepository productRepository;

    @Override
    @Transactional
    public OrderResponse placeOrder(Long userId, PlaceOrderRequest req) {
        Cart cart = cartRepository.findByUserIdWithItems(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));
        if (cart.getItems().isEmpty())
            throw new BusinessException("Cart is empty");

        Address address = addressRepository.findById(req.addressId())
                .orElseThrow(() -> new ResourceNotFoundException("Address not found: " + req.addressId()));
        if (!address.getUser().getId().equals(userId))
            throw new BusinessException("Address does not belong to this user");

        Order order = new Order();
        order.setOrderNumber("P5-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        order.setUser(cart.getUser());
        order.setShippingAddressSnapshot(formatAddress(address));
        order.setCouponCode(req.couponCode());
        order.setNotes(req.notes());

        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem cartItem : cart.getItems()) {
            Product product = cartItem.getProduct();
            int qty = cartItem.getQuantity();
            if (product.getStockQuantity() < qty)
                throw new BusinessException("Insufficient stock for: " + product.getName());

            OrderItem oi = new OrderItem();
            oi.setOrder(order);
            oi.setProduct(product);
            oi.setProductName(product.getName());
            oi.setProductSku(product.getSku());
            oi.setQuantity(qty);
            oi.setUnitPrice(product.getPrice());
            oi.setSubtotal(product.getPrice().multiply(BigDecimal.valueOf(qty)));
            order.getItems().add(oi);

            product.setStockQuantity(product.getStockQuantity() - qty);
            if (product.getStockQuantity() == 0)
                product.setStatus(Product.ProductStatus.OUT_OF_STOCK);
            productRepository.save(product);

            subtotal = subtotal.add(oi.getSubtotal());
        }

        BigDecimal shipping = subtotal.compareTo(FREE_SHIPPING_THRESHOLD) >= 0 ? BigDecimal.ZERO : SHIPPING_COST;
        order.setSubtotal(subtotal);
        order.setShippingCost(shipping);
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setTotal(subtotal.add(shipping));

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setAmount(order.getTotal());
        payment.setMethod(Payment.PaymentMethod.valueOf(req.paymentMethod().toUpperCase()));
        payment.setStatus(Payment.PaymentStatus.PENDING);
        order.setPayment(payment);

        Order saved = orderRepository.save(order);
        cart.getItems().clear();
        cartRepository.save(cart);

        return toResponse(saved);
    }

    @Override
    public List<OrderResponse> getUserOrders(Long userId) {
        return orderRepository.findByUserIdWithItems(userId).stream().map(this::toResponse).toList();
    }

    @Override
    public OrderResponse getById(Long orderId) {
        return toResponse(findOrder(orderId));
    }

    @Override
    public OrderResponse getByOrderNumber(String orderNumber) {
        return toResponse(orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderNumber)));
    }

    @Override
    @Transactional
    public OrderResponse updateStatus(Long orderId, Order.OrderStatus status) {
        Order order = findOrder(orderId);
        order.setStatus(status);
        return toResponse(orderRepository.save(order));
    }

    @Override
    @Transactional
    public OrderResponse cancelOrder(Long userId, Long orderId) {
        Order order = findOrder(orderId);
        if (!order.getUser().getId().equals(userId))
            throw new BusinessException("Order does not belong to this user");
        if (order.getStatus() != Order.OrderStatus.PENDING && order.getStatus() != Order.OrderStatus.CONFIRMED)
            throw new BusinessException("Order cannot be cancelled in status: " + order.getStatus());

        order.setStatus(Order.OrderStatus.CANCELLED);
        order.getItems().forEach(oi -> {
            if (oi.getProduct() != null) {
                Product p = oi.getProduct();
                p.setStockQuantity(p.getStockQuantity() + oi.getQuantity());
                if (p.getStatus() == Product.ProductStatus.OUT_OF_STOCK)
                    p.setStatus(Product.ProductStatus.ACTIVE);
                productRepository.save(p);
            }
        });
        return toResponse(orderRepository.save(order));
    }

    private Order findOrder(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));
    }

    private String formatAddress(Address a) {
        return String.format("%s, %s, %s %s, %s", a.getStreet(), a.getCity(),
                a.getProvince() != null ? a.getProvince() + "," : "", a.getPostalCode(), a.getCountry());
    }

    OrderResponse toResponse(Order o) {
        List<OrderResponse.OrderItemResponse> items = o.getItems().stream()
                .map(i -> new OrderResponse.OrderItemResponse(
                        i.getProductName(), i.getProductSku(), i.getQuantity(), i.getUnitPrice(), i.getSubtotal()))
                .toList();
        return new OrderResponse(o.getId(), o.getOrderNumber(), o.getStatus().name(),
                o.getSubtotal(), o.getShippingCost(), o.getDiscountAmount(), o.getTotal(),
                o.getShippingAddressSnapshot(), o.getCreatedAt(), items);
    }
}
