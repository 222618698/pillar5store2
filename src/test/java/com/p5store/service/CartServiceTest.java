package com.p5store.service;

import com.p5store.domain.*;
import com.p5store.exception.BadRequestException;
import com.p5store.exception.ResourceNotFoundException;
import com.p5store.repository.CartRepository;
import com.p5store.repository.ProductVariantRepository;
import com.p5store.repository.UserRepository;
import com.p5store.service.impl.CartServiceImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CartService")
class CartServiceTest {

    @Mock CartRepository cartRepository;
    @Mock UserRepository userRepository;
    @Mock ProductVariantRepository variantRepository;
    @InjectMocks CartServiceImpl cartService;

    private UUID userId;
    private UUID variantId;
    private User user;
    private ProductVariant variant;
    private Cart cart;

    @BeforeEach
    void setUp() {
        userId    = UUID.randomUUID();
        variantId = UUID.randomUUID();

        user = User.builder().firstName("John").lastName("Doe").email("john@test.com").build();

        Product product = Product.builder()
                .name("Test Product")
                .basePrice(new BigDecimal("100.00"))
                .build();

        variant = ProductVariant.builder()
                .product(product)
                .size("M")
                .colour("Blue")
                .priceModifier(BigDecimal.ZERO)
                .stockQuantity(10)
                .build();
        // reflectively set id for lookups
        setId(variant, variantId);

        cart = Cart.builder().user(user).build();
    }

    // ── getOrCreateCart ────────────────────────────────────────
    @Nested
    @DisplayName("getOrCreateCart")
    class GetOrCreateCart {

        @Test
        @DisplayName("returns existing cart when found")
        void returnsExisting_whenFound() {
            given(cartRepository.findByUserIdWithItems(userId)).willReturn(Optional.of(cart));

            Cart result = cartService.getOrCreateCart(userId);

            assertThat(result).isSameAs(cart);
            then(userRepository).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("creates new cart when none exists")
        void createsNew_whenNotFound() {
            given(cartRepository.findByUserIdWithItems(userId)).willReturn(Optional.empty());
            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(cartRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            Cart result = cartService.getOrCreateCart(userId);

            assertThat(result).isNotNull();
            assertThat(result.getUser()).isEqualTo(user);
            then(cartRepository).should().save(any(Cart.class));
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when user not found")
        void throwsNotFound_whenUserMissing() {
            given(cartRepository.findByUserIdWithItems(userId)).willReturn(Optional.empty());
            given(userRepository.findById(userId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> cartService.getOrCreateCart(userId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ── addItem ────────────────────────────────────────────────
    @Nested
    @DisplayName("addItem")
    class AddItem {

        @Test
        @DisplayName("adds new item to empty cart")
        void addsNewItem_toEmptyCart() {
            given(cartRepository.findByUserIdWithItems(userId)).willReturn(Optional.of(cart));
            given(variantRepository.findById(variantId)).willReturn(Optional.of(variant));
            given(cartRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            Cart result = cartService.addItem(userId, variantId, 2);

            assertThat(result.getItems()).hasSize(1);
            assertThat(result.getItems().get(0).getQuantity()).isEqualTo(2);
        }

        @Test
        @DisplayName("increments quantity when variant already in cart")
        void incrementsQuantity_whenVariantAlreadyInCart() {
            // Pre-populate cart with 1 of this variant
            CartItem existingItem = CartItem.builder()
                    .cart(cart).productVariant(variant).quantity(1).build();
            cart.getItems().add(existingItem);

            given(cartRepository.findByUserIdWithItems(userId)).willReturn(Optional.of(cart));
            given(variantRepository.findById(variantId)).willReturn(Optional.of(variant));
            given(cartRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            Cart result = cartService.addItem(userId, variantId, 3);

            assertThat(result.getItems()).hasSize(1);
            assertThat(result.getItems().get(0).getQuantity()).isEqualTo(4); // 1 + 3
        }

        @Test
        @DisplayName("throws BadRequestException when quantity <= 0")
        void throwsBadRequest_whenQuantityZero() {
            assertThatThrownBy(() -> cartService.addItem(userId, variantId, 0))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Quantity");
        }

        @Test
        @DisplayName("throws BadRequestException when variant is out of stock")
        void throwsBadRequest_whenOutOfStock() {
            variant.setStockQuantity(0);
            given(cartRepository.findByUserIdWithItems(userId)).willReturn(Optional.of(cart));
            given(variantRepository.findById(variantId)).willReturn(Optional.of(variant));

            assertThatThrownBy(() -> cartService.addItem(userId, variantId, 1))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("out of stock");
        }

        @Test
        @DisplayName("throws BadRequestException when requested qty exceeds stock")
        void throwsBadRequest_whenExceedsStock() {
            given(cartRepository.findByUserIdWithItems(userId)).willReturn(Optional.of(cart));
            given(variantRepository.findById(variantId)).willReturn(Optional.of(variant));

            assertThatThrownBy(() -> cartService.addItem(userId, variantId, 99))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("10");
        }
    }

    // ── removeItem ─────────────────────────────────────────────
    @Nested
    @DisplayName("removeItem")
    class RemoveItem {

        @Test
        @DisplayName("removes item from cart")
        void removesItem() {
            CartItem item = CartItem.builder()
                    .cart(cart).productVariant(variant).quantity(2).build();
            cart.getItems().add(item);

            given(cartRepository.findByUserIdWithItems(userId)).willReturn(Optional.of(cart));
            given(cartRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            Cart result = cartService.removeItem(userId, variantId);

            assertThat(result.getItems()).isEmpty();
        }
    }

    // ── clearCart ──────────────────────────────────────────────
    @Nested
    @DisplayName("clearCart")
    class ClearCart {

        @Test
        @DisplayName("removes all items")
        void removesAllItems() {
            cart.getItems().add(CartItem.builder().cart(cart).productVariant(variant).quantity(1).build());
            given(cartRepository.findByUserIdWithItems(userId)).willReturn(Optional.of(cart));
            given(cartRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            cartService.clearCart(userId);

            assertThat(cart.getItems()).isEmpty();
        }
    }

    // ── Helper — set id on entity via reflection ───────────────
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
