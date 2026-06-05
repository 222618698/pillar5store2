package com.p5store.service;

import com.p5store.config.ProductMapper;
import com.p5store.domain.*;
import com.p5store.dto.request.ProductRequest;
import com.p5store.dto.response.ProductResponse;
import com.p5store.exception.DuplicateResourceException;
import com.p5store.exception.ResourceNotFoundException;
import com.p5store.repository.CategoryRepository;
import com.p5store.repository.ProductRepository;
import com.p5store.repository.ProductVariantRepository;
import com.p5store.service.impl.AdminProductServiceImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminProductService")
class AdminProductServiceTest {

    @Mock ProductRepository productRepository;
    @Mock CategoryRepository categoryRepository;
    @Mock ProductVariantRepository variantRepository;
    @Mock ProductMapper productMapper;
    @InjectMocks AdminProductServiceImpl adminProductService;

    private UUID categoryId;
    private Category category;
    private ProductRequest validRequest;

    @BeforeEach
    void setUp() {
        categoryId = UUID.randomUUID();
        category = Category.builder()
                .name("Jewellery")
                .slug("jewellery")
                .isActive(true)
                .build();

        validRequest = new ProductRequest(
                categoryId,
                "Twisted Wire Cuff",
                "JWL-999",
                "Handcrafted twisted wire cuff",
                new BigDecimal("349.00"),
                List.of(new ProductRequest.VariantRequest(
                        "One Size", "Silver", BigDecimal.ZERO, 20, null)),
                List.of(new ProductRequest.ImageRequest(
                        "https://cdn.p5store.com/images/cuff.jpg", true, 0)),
                true
        );
    }

    // ── createProduct ──────────────────────────────────────────
    @Nested
    @DisplayName("createProduct")
    class CreateProduct {

        @Test
        @DisplayName("creates product and returns ProductResponse when SKU is unique")
        void createsProduct_whenSkuUnique() {
            Product builtProduct = Product.builder()
                    .name("Twisted Wire Cuff")
                    .sku("JWL-999")
                    .basePrice(new BigDecimal("349.00"))
                    .category(category)
                    .isActive(true)
                    .build();

            ProductResponse expectedResponse = new ProductResponse(
                    UUID.randomUUID(), "Twisted Wire Cuff", "twisted-wire-cuff",
                    "Handcrafted twisted wire cuff", "JWL-999", new BigDecimal("349.00"),
                    true, new ProductResponse.CategorySummary(categoryId, "Jewellery", "jewellery"),
                    List.of(), List.of(), null, 0, null, null);

            given(productRepository.existsBySku("JWL-999")).willReturn(false);
            given(categoryRepository.findById(categoryId)).willReturn(Optional.of(category));
            given(productMapper.toDomain(validRequest, category)).willReturn(builtProduct);
            given(productRepository.save(builtProduct)).willReturn(builtProduct);
            given(productMapper.toResponse(builtProduct)).willReturn(expectedResponse);

            ProductResponse result = adminProductService.createProduct(validRequest);

            assertThat(result).isNotNull();
            assertThat(result.name()).isEqualTo("Twisted Wire Cuff");
            assertThat(result.sku()).isEqualTo("JWL-999");
            assertThat(result.active()).isTrue();
        }

        @Test
        @DisplayName("throws DuplicateResourceException when SKU already exists")
        void throwsDuplicate_whenSkuExists() {
            given(productRepository.existsBySku("JWL-999")).willReturn(true);

            assertThatThrownBy(() -> adminProductService.createProduct(validRequest))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("JWL-999");

            then(productRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when category not found")
        void throwsNotFound_whenCategoryMissing() {
            given(productRepository.existsBySku(anyString())).willReturn(false);
            given(categoryRepository.findById(categoryId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> adminProductService.createProduct(validRequest))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining(categoryId.toString());
        }
    }

    // ── updateProduct ──────────────────────────────────────────
    @Nested
    @DisplayName("updateProduct")
    class UpdateProduct {

        @Test
        @DisplayName("updates fields and returns updated ProductResponse")
        void updatesProduct() {
            UUID productId = UUID.randomUUID();
            Product existing = Product.builder()
                    .name("Old Name")
                    .sku("OLD-001")
                    .basePrice(new BigDecimal("100.00"))
                    .category(category)
                    .isActive(true)
                    .build();

            ProductResponse expectedResponse = new ProductResponse(
                    productId, "Twisted Wire Cuff", "twisted-wire-cuff",
                    "Handcrafted twisted wire cuff", "JWL-999", new BigDecimal("349.00"),
                    true, new ProductResponse.CategorySummary(categoryId, "Jewellery", "jewellery"),
                    List.of(), List.of(), null, 0, null, null);

            given(productRepository.findById(productId)).willReturn(Optional.of(existing));
            given(categoryRepository.findById(categoryId)).willReturn(Optional.of(category));
            given(productRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(productMapper.toResponse(any())).willReturn(expectedResponse);

            ProductResponse result = adminProductService.updateProduct(productId, validRequest);

            assertThat(result.name()).isEqualTo("Twisted Wire Cuff");
            assertThat(existing.getName()).isEqualTo("Twisted Wire Cuff");
            assertThat(existing.getBasePrice()).isEqualByComparingTo("349.00");
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when product not found")
        void throwsNotFound_whenProductMissing() {
            UUID productId = UUID.randomUUID();
            given(productRepository.findById(productId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> adminProductService.updateProduct(productId, validRequest))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ── deleteProduct ──────────────────────────────────────────
    @Nested
    @DisplayName("deleteProduct")
    class DeleteProduct {

        @Test
        @DisplayName("sets isActive=false and saves — product hidden from users")
        void softDeletesProduct() {
            UUID productId = UUID.randomUUID();
            Product product = Product.builder()
                    .name("To Delete").sku("DEL-001")
                    .basePrice(BigDecimal.TEN).category(category)
                    .isActive(true).build();

            given(productRepository.findById(productId)).willReturn(Optional.of(product));
            given(productRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            adminProductService.deleteProduct(productId);

            assertThat(product.isActive()).isFalse();
            then(productRepository).should().save(product);
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when product not found")
        void throwsNotFound_whenProductMissing() {
            UUID productId = UUID.randomUUID();
            given(productRepository.findById(productId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> adminProductService.deleteProduct(productId))
                    .isInstanceOf(ResourceNotFoundException.class);

            then(productRepository).should(never()).save(any());
        }
    }
}
