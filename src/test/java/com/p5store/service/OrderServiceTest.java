package com.p5store.service;

import com.p5store.domain.*;
import com.p5store.enums.DiscountType;
import com.p5store.enums.OrderStatus;
import com.p5store.exception.*;
import com.p5store.repository.*;
import com.p5store.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService")
class OrderServiceTest {

    @Mock OrderRepository orderRepository;
    @Mock UserRepository userRepository;
    @Mock AddressRepository addressRepository;
    @Mock CartRepository cartRepository;
    @Mock DiscountRepository discountRepository;
    @Mock CartService cartService;
    @InjectMocks OrderServiceImpl orderService;

    private UUID userId;
    private UUID addressId;
    private User user;
    private Address address;
    private Cart cart;
    private Product product;
    private ProductVariant variant;

    @BeforeEach
    void setUp() {
        userId    = UUID.randomUUID();
        addressId = UUID.randomUUID();

        user = User.builder().firstName("Jane").lastName("Smith").email("jane@test.com").build();
        setId(user, userId);

        address = Address.builder().user(user).street("1 Test Rd").city("Cape Town")
                .province("WC").postalCode("7550").build();
        setId(address, addressId);

        product = Product.builder()
                .name("Test Product")
                .basePrice(new BigDecimal("200.00"))
                .build();

        variant = ProductVariant.builder()
                .product(product)
                .priceModifier(BigDecimal.ZERO)
                .stockQuantity(5)
                .build();

        CartItem cartItem = CartItem.builder().productVariant(variant).quantity(2).build();

        cart = Cart.builder().user(user).build();
        cart.getItems().add(cartItem);
        cartItem.setCart(cart);
    }

    // ── placeOrder ─────────────────────────────────────────────
    @Nested
    @DisplayName("placeOrder")
    class PlaceOrder {

        @BeforeEach
        void mockFoundEntities() {
            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(addressRepository.findById(addressId)).willReturn(Optional.of(address));
            given(cartRepository.findByUserIdWithItems(userId)).willReturn(Optional.of(cart));
            given(orderRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(cartRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
        }

        @Test
        @DisplayName("creates order with correct subtotal from cart items")
        void createsOrderWithCorrectSubtotal() {
            Order result = orderService.placeOrder(userId, addressId, null);

            // 2 × R200 = R400 subtotal, shipping = R79 (under R500 threshold)
            assertThat(result.getSubtotal()).isEqualByComparingTo("400.00");
            assertThat(result.getShippingCost()).isEqualByComparingTo("79.00");
            assertThat(result.getTotal()).isEqualByComparingTo("479.00");
        }

        @Test
        @DisplayName("applies free shipping when subtotal >= R500")
        void freeShipping_whenSubtotalOverThreshold() {
            variant.setStockQuantity(10);
            cart.getItems().get(0).setQuantity(3); // 3 × R200 = R600

            Order result = orderService.placeOrder(userId, addressId, null);

            assertThat(result.getSubtotal()).isEqualByComparingTo("600.00");
            assertThat(result.getShippingCost()).isEqualByComparingTo("0.00");
        }

        @Test
        @DisplayName("deducts stock on successful order placement")
        void deductsStock_onSuccess() {
            int stockBefore = variant.getStockQuantity(); // 5

            orderService.placeOrder(userId, addressId, null);

            assertThat(variant.getStockQuantity()).isEqualTo(stockBefore - 2); // deducted 2
        }

        @Test
        @DisplayName("clears cart after order placed")
        void clearsCart_afterOrderPlaced() {
            orderService.placeOrder(userId, addressId, null);

            assertThat(cart.getItems()).isEmpty();
        }

        @Test
        @DisplayName("generates unique order number with P5- prefix")
        void generatesOrderNumber_withPrefix() {
            Order result = orderService.placeOrder(userId, addressId, null);

            assertThat(result.getOrderNumber()).startsWith("P5-");
        }

        @Test
        @DisplayName("applies percentage discount correctly")
        void appliesPercentageDiscount() {
            Discount discount = Discount.builder()
                    .code("SAVE15")
                    .type(DiscountType.PERCENTAGE)
                    .value(new BigDecimal("15"))
                    .minOrderValue(BigDecimal.ZERO)
                    .isActive(true)
                    .build();

            given(discountRepository.findByCodeIgnoreCase("SAVE15")).willReturn(Optional.of(discount));

            Order result = orderService.placeOrder(userId, addressId, "SAVE15");

            // 15% of R400 = R60 discount
            assertThat(result.getDiscountAmount()).isEqualByComparingTo("60.00");
        }

        @Test
        @DisplayName("throws InvalidDiscountException for unknown code")
        void throwsInvalidDiscount_forUnknownCode() {
            given(discountRepository.findByCodeIgnoreCase("FAKE")).willReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.placeOrder(userId, addressId, "FAKE"))
                    .isInstanceOf(InvalidDiscountException.class);
        }

        @Test
        @DisplayName("throws InsufficientStockException when stock too low")
        void throwsInsufficientStock_whenStockTooLow() {
            variant.setStockQuantity(1); // only 1 available but 2 requested

            assertThatThrownBy(() -> orderService.placeOrder(userId, addressId, null))
                    .isInstanceOf(InsufficientStockException.class);
        }

        @Test
        @DisplayName("throws BadRequestException when cart is empty")
        void throwsBadRequest_whenCartEmpty() {
            cart.getItems().clear();

            assertThatThrownBy(() -> orderService.placeOrder(userId, addressId, null))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("empty");
        }

        @Test
        @DisplayName("throws BadRequestException when address belongs to different user")
        void throwsBadRequest_whenAddressNotOwned() {
            User otherUser = User.builder().firstName("Other").lastName("User").email("other@test.com").build();
            setId(otherUser, UUID.randomUUID());
            address.setUser(otherUser); // address owned by someone else

            assertThatThrownBy(() -> orderService.placeOrder(userId, addressId, null))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("does not belong");
        }
    }

    // ── cancel ─────────────────────────────────────────────────
    @Nested
    @DisplayName("cancel")
    class Cancel {

        private Order pendingOrder;
        private UUID orderId;

        @BeforeEach
        void setUpOrder() {
            orderId = UUID.randomUUID();
            pendingOrder = Order.builder()
                    .user(user)
                    .status(OrderStatus.PENDING)
                    .subtotal(new BigDecimal("400.00"))
                    .shippingCost(new BigDecimal("79.00"))
                    .discountAmount(BigDecimal.ZERO)
                    .total(new BigDecimal("479.00"))
                    .orderNumber("P5-20260101-1234")
                    .shippingAddress(address)
                    .build();

            OrderItem oi = OrderItem.builder()
                    .order(pendingOrder)
                    .productVariant(variant)
                    .quantity(2)
                    .unitPrice(new BigDecimal("200.00"))
                    .lineTotal(new BigDecimal("400.00"))
                    .build();
            pendingOrder.getItems().add(oi);

            given(orderRepository.findByIdWithItems(orderId)).willReturn(Optional.of(pendingOrder));
            given(orderRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
        }

        @Test
        @DisplayName("cancels pending order and returns stock")
        void cancelsPendingOrder_andReturnsStock() {
            int stockBefore = variant.getStockQuantity();

            Order result = orderService.cancel(orderId, userId);

            assertThat(result.getStatus()).isEqualTo(OrderStatus.CANCELLED);
            assertThat(variant.getStockQuantity()).isEqualTo(stockBefore + 2);
        }

        @Test
        @DisplayName("throws BadRequestException when order is already shipped")
        void throwsBadRequest_whenAlreadyShipped() {
            pendingOrder.setStatus(OrderStatus.SHIPPED);

            assertThatThrownBy(() -> orderService.cancel(orderId, userId))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("cannot be cancelled");
        }

        @Test
        @DisplayName("throws BadRequestException when a different user tries to cancel")
        void throwsBadRequest_whenWrongUser() {
            UUID otherUserId = UUID.randomUUID();

            assertThatThrownBy(() -> orderService.cancel(orderId, otherUserId))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Cannot cancel another user");
        }
    }

    // ── updateStatus ───────────────────────────────────────────
    @Nested
    @DisplayName("updateStatus")
    class UpdateStatus {

        @Test
        @DisplayName("updates order status correctly")
        void updatesStatus() {
            UUID orderId = UUID.randomUUID();
            Order order = Order.builder()
                    .status(OrderStatus.PENDING)
                    .user(user).shippingAddress(address)
                    .subtotal(BigDecimal.TEN).shippingCost(BigDecimal.ZERO)
                    .discountAmount(BigDecimal.ZERO).total(BigDecimal.TEN)
                    .orderNumber("P5-TEST-001")
                    .build();

            given(orderRepository.findByIdWithItems(orderId)).willReturn(Optional.of(order));
            given(orderRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            Order result = orderService.updateStatus(orderId, OrderStatus.SHIPPED);

            assertThat(result.getStatus()).isEqualTo(OrderStatus.SHIPPED);
        }
    }

    // ── Helper ─────────────────────────────────────────────────
    private void setId(Object entity, UUID id) {
        try {
            var f = com.p5store.domain.BaseEntity.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
