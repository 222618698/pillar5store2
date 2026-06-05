package com.p5store.service.impl;

import com.p5store.config.ProductMapper;
import com.p5store.domain.*;
import com.p5store.dto.request.ProductRequest;
import com.p5store.dto.response.ProductResponse;
import com.p5store.exception.DuplicateResourceException;
import com.p5store.exception.ResourceNotFoundException;
import com.p5store.repository.*;
import com.p5store.service.AdminProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AdminProductServiceImpl implements AdminProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductMapper productMapper;

    // ── CREATE ──────────────────────────────────────────────────
    @Override
    public ProductResponse createProduct(ProductRequest request) {
        // 1. Guard duplicate SKU
        if (productRepository.existsBySku(request.sku().toUpperCase())) {
            throw new DuplicateResourceException("SKU already exists: " + request.sku());
        }

        // 2. Resolve category
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", request.categoryId()));

        // 3. Build domain object (with variants + images attached)
        Product product = productMapper.toDomain(request, category);

        // 4. Persist — cascade saves variants and images
        Product saved = productRepository.save(product);

        log.info("Admin created product '{}' (SKU: {}) in category '{}'",
                saved.getName(), saved.getSku(), category.getName());

        return productMapper.toResponse(saved);
    }

    // ── UPDATE ──────────────────────────────────────────────────
    @Override
    public ProductResponse updateProduct(UUID id, ProductRequest request) {
        Product existing = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));

        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", request.categoryId()));

        // Update scalar fields
        existing.setName(request.name());
        existing.setDescription(request.description());
        existing.setBasePrice(request.basePrice());
        existing.setCategory(category);
        existing.setActive(request.active());
        existing.setSku(request.sku().toUpperCase());

        // Replace variants — orphanRemoval will delete old ones
        existing.getVariants().clear();
        if (request.variants() != null) {
            request.variants().forEach(v -> existing.getVariants().add(
                    ProductVariant.builder()
                            .product(existing)
                            .size(v.size())
                            .colour(v.colour())
                            .priceModifier(v.priceModifier())
                            .stockQuantity(v.stockQuantity())
                            .imageUrl(v.imageUrl())
                            .build()));
        }

        // Replace images
        existing.getImages().clear();
        if (request.images() != null) {
            request.images().forEach(img -> existing.getImages().add(
                    ProductImage.builder()
                            .product(existing)
                            .url(img.url())
                            .isPrimary(img.primary())
                            .sortOrder(img.sortOrder())
                            .build()));
        }

        Product saved = productRepository.save(existing);
        log.info("Admin updated product '{}' (id: {})", saved.getName(), id);
        return productMapper.toResponse(saved);
    }

    // ── SOFT DELETE ─────────────────────────────────────────────
    @Override
    public void deleteProduct(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
        product.setActive(false);
        productRepository.save(product);
        log.info("Admin soft-deleted product '{}' (id: {})", product.getName(), id);
    }

    // ── LIST ALL (including inactive) ────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> listAll(Pageable pageable) {
        return productRepository.findAll(pageable)
                .map(productMapper::toResponse);
    }
}
