package com.p5store.service;

import com.p5store.domain.Category;
import com.p5store.domain.Product;
import com.p5store.exception.DuplicateResourceException;
import com.p5store.exception.ResourceNotFoundException;
import com.p5store.repository.ProductRepository;
import com.p5store.service.impl.ProductServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductService")
class ProductServiceTest {

    @Mock ProductRepository productRepository;
    @InjectMocks ProductServiceImpl productService;

    private Product sampleProduct;
    private UUID productId;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();
        Category category = Category.builder()
                .name("Jewellery")
                .slug("jewellery")
                .build();

        sampleProduct = Product.builder()
                .name("Gold Bracelet")
                .slug("gold-bracelet")
                .description("A beautiful gold bracelet")
                .basePrice(new BigDecimal("207.86"))
                .sku("JWL-001")
                .category(category)
                .isActive(true)
                .build();
    }

    // ── findById ───────────────────────────────────────────────
    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("returns product when it exists")
        void returnsProduct_whenExists() {
            given(productRepository.findById(productId)).willReturn(Optional.of(sampleProduct));

            Product result = productService.findById(productId);

            assertThat(result).isEqualTo(sampleProduct);
            assertThat(result.getName()).isEqualTo("Gold Bracelet");
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when not found")
        void throwsNotFound_whenMissing() {
            given(productRepository.findById(productId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> productService.findById(productId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining(productId.toString());
        }
    }

    // ── findBySlug ─────────────────────────────────────────────
    @Nested
    @DisplayName("findBySlug")
    class FindBySlug {

        @Test
        @DisplayName("returns product when slug matches")
        void returnsProduct_whenSlugMatches() {
            given(productRepository.findBySlug("gold-bracelet")).willReturn(Optional.of(sampleProduct));

            Product result = productService.findBySlug("gold-bracelet");

            assertThat(result.getSlug()).isEqualTo("gold-bracelet");
        }

        @Test
        @DisplayName("throws ResourceNotFoundException for unknown slug")
        void throwsNotFound_forUnknownSlug() {
            given(productRepository.findBySlug(anyString())).willReturn(Optional.empty());

            assertThatThrownBy(() -> productService.findBySlug("missing"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ── create ─────────────────────────────────────────────────
    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("saves and returns product when SKU is unique")
        void savesProduct_whenSkuIsUnique() {
            given(productRepository.existsBySku("JWL-001")).willReturn(false);
            given(productRepository.save(sampleProduct)).willReturn(sampleProduct);

            Product result = productService.create(sampleProduct);

            assertThat(result).isEqualTo(sampleProduct);
            then(productRepository).should().save(sampleProduct);
        }

        @Test
        @DisplayName("throws DuplicateResourceException when SKU already exists")
        void throwsDuplicate_whenSkuExists() {
            given(productRepository.existsBySku("JWL-001")).willReturn(true);

            assertThatThrownBy(() -> productService.create(sampleProduct))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("JWL-001");

            then(productRepository).should(never()).save(any());
        }
    }

    // ── update ─────────────────────────────────────────────────
    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("updates mutable fields and saves")
        void updatesMutableFields() {
            given(productRepository.findById(productId)).willReturn(Optional.of(sampleProduct));
            given(productRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            Product updates = Product.builder()
                    .name("Platinum Bracelet")
                    .description("Updated")
                    .basePrice(new BigDecimal("350.00"))
                    .category(sampleProduct.getCategory())
                    .isActive(true)
                    .build();

            Product result = productService.update(productId, updates);

            assertThat(result.getName()).isEqualTo("Platinum Bracelet");
            assertThat(result.getBasePrice()).isEqualByComparingTo("350.00");
        }
    }

    // ── delete (soft) ──────────────────────────────────────────
    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("sets isActive to false (soft delete)")
        void softDeletesSetsInactive() {
            given(productRepository.findById(productId)).willReturn(Optional.of(sampleProduct));
            given(productRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            productService.delete(productId);

            assertThat(sampleProduct.isActive()).isFalse();
            then(productRepository).should().save(sampleProduct);
        }
    }

    // ── search ─────────────────────────────────────────────────
    @Nested
    @DisplayName("search")
    class Search {

        @Test
        @DisplayName("delegates to repository with trimmed query")
        void delegatesSearch_withTrimmedQuery() {
            Page<Product> page = new PageImpl<>(List.of(sampleProduct));
            PageRequest pageable = PageRequest.of(0, 20);
            given(productRepository.search(eq("bracelet"), eq(pageable))).willReturn(page);

            Page<Product> result = productService.search("  bracelet  ", pageable);

            assertThat(result.getContent()).hasSize(1);
            then(productRepository).should().search("bracelet", pageable);
        }
    }
}
