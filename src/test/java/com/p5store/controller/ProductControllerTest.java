package com.p5store.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.p5store.config.JwtService;
import com.p5store.domain.Category;
import com.p5store.domain.Product;
import com.p5store.exception.ResourceNotFoundException;
import com.p5store.service.ProductService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
@DisplayName("ProductController")
class ProductControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean ProductService productService;
    @MockBean JwtService jwtService;
    @MockBean UserDetailsService userDetailsService;

    private Product sampleProduct;
    private UUID productId;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();
        Category category = Category.builder().name("Jewellery").slug("jewellery").build();
        sampleProduct = Product.builder()
                .name("Gold Bracelet")
                .slug("gold-bracelet")
                .description("Elegant gold bracelet")
                .basePrice(new BigDecimal("207.86"))
                .sku("JWL-001")
                .category(category)
                .isActive(true)
                .build();
    }

    // ── GET /products ──────────────────────────────────────────
    @Nested
    @DisplayName("GET /products")
    class ListProducts {

        @Test
        @DisplayName("returns 200 with product page — no auth required")
        void returns200_noAuthRequired() throws Exception {
            var page = new PageImpl<>(List.of(sampleProduct), PageRequest.of(0, 20), 1);
            given(productService.findAll(any())).willReturn(page);

            mockMvc.perform(get("/products"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content[0].name").value("Gold Bracelet"))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        @DisplayName("delegates to search when ?q param provided")
        void delegatesToSearch_whenQueryParam() throws Exception {
            var page = new PageImpl<>(List.of(sampleProduct), PageRequest.of(0, 20), 1);
            given(productService.search(eq("bracelet"), any())).willReturn(page);

            mockMvc.perform(get("/products").param("q", "bracelet"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].name").value("Gold Bracelet"));

            then(productService).should().search(eq("bracelet"), any());
        }

        @Test
        @DisplayName("delegates to category filter when ?categoryId param provided")
        void delegatesToCategoryFilter_whenCategoryIdParam() throws Exception {
            UUID categoryId = UUID.randomUUID();
            var page = new PageImpl<>(List.of(sampleProduct), PageRequest.of(0, 20), 1);
            given(productService.findByCategory(eq(categoryId), any())).willReturn(page);

            mockMvc.perform(get("/products").param("categoryId", categoryId.toString()))
                    .andExpect(status().isOk());

            then(productService).should().findByCategory(eq(categoryId), any());
        }
    }

    // ── GET /products/{id} ─────────────────────────────────────
    @Nested
    @DisplayName("GET /products/{id}")
    class GetById {

        @Test
        @DisplayName("returns 200 with product details")
        void returns200_withProduct() throws Exception {
            given(productService.findById(productId)).willReturn(sampleProduct);

            mockMvc.perform(get("/products/{id}", productId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Gold Bracelet"))
                    .andExpect(jsonPath("$.sku").value("JWL-001"));
        }

        @Test
        @DisplayName("returns 404 when product not found")
        void returns404_whenNotFound() throws Exception {
            given(productService.findById(any())).willThrow(
                    new ResourceNotFoundException("Product", productId));

            mockMvc.perform(get("/products/{id}", productId))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").exists());
        }
    }

    // ── GET /products/slug/{slug} ──────────────────────────────
    @Nested
    @DisplayName("GET /products/slug/{slug}")
    class GetBySlug {

        @Test
        @DisplayName("returns 200 with product for valid slug")
        void returns200_forValidSlug() throws Exception {
            given(productService.findBySlug("gold-bracelet")).willReturn(sampleProduct);

            mockMvc.perform(get("/products/slug/gold-bracelet"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.slug").value("gold-bracelet"));
        }
    }

    // ── POST /products  (admin only) ───────────────────────────
    @Nested
    @DisplayName("POST /products — admin only")
    class CreateProduct {

        @Test
        @DisplayName("returns 201 when admin creates product")
        @WithMockUser(roles = "ADMIN")
        void returns201_whenAdminCreates() throws Exception {
            given(productService.create(any())).willReturn(sampleProduct);

            mockMvc.perform(post("/products")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(sampleProduct)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value("Gold Bracelet"));
        }

        @Test
        @DisplayName("returns 403 when customer tries to create product")
        @WithMockUser(roles = "CUSTOMER")
        void returns403_whenCustomerTriesToCreate() throws Exception {
            mockMvc.perform(post("/products")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(sampleProduct)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("returns 403 when unauthenticated user tries to create product")
        void returns403_whenUnauthenticated() throws Exception {
            mockMvc.perform(post("/products")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(sampleProduct)))
                    .andExpect(status().isForbidden());
        }
    }

    // ── PUT /products/{id}  (admin only) ──────────────────────
    @Nested
    @DisplayName("PUT /products/{id} — admin only")
    class UpdateProduct {

        @Test
        @DisplayName("returns 200 when admin updates product")
        @WithMockUser(roles = "ADMIN")
        void returns200_whenAdminUpdates() throws Exception {
            given(productService.update(eq(productId), any())).willReturn(sampleProduct);

            mockMvc.perform(put("/products/{id}", productId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(sampleProduct)))
                    .andExpect(status().isOk());
        }
    }

    // ── DELETE /products/{id}  (admin only) ───────────────────
    @Nested
    @DisplayName("DELETE /products/{id} — admin only")
    class DeleteProduct {

        @Test
        @DisplayName("returns 204 when admin soft-deletes product")
        @WithMockUser(roles = "ADMIN")
        void returns204_whenAdminDeletes() throws Exception {
            willDoNothing().given(productService).delete(productId);

            mockMvc.perform(delete("/products/{id}", productId).with(csrf()))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("returns 403 when customer tries to delete")
        @WithMockUser(roles = "CUSTOMER")
        void returns403_whenCustomerDeletes() throws Exception {
            mockMvc.perform(delete("/products/{id}", productId).with(csrf()))
                    .andExpect(status().isForbidden());
        }
    }
}
