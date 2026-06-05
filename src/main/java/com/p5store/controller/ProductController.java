package com.p5store.controller;

import com.p5store.config.ProductMapper;
import com.p5store.dto.request.ProductRequest;
import com.p5store.dto.response.ProductResponse;
import com.p5store.service.AdminProductService;
import com.p5store.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final AdminProductService adminProductService;
    private final ProductMapper productMapper;

    // ── PUBLIC: customers browse active catalogue ────────────────

    @GetMapping
    public ResponseEntity<Page<ProductResponse>> list(
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {

        Page<ProductResponse> result;
        if (q != null && !q.isBlank()) {
            result = productService.search(q, pageable).map(productMapper::toResponse);
        } else if (categoryId != null) {
            result = productService.findByCategory(categoryId, pageable).map(productMapper::toResponse);
        } else {
            result = productService.findAll(pageable).map(productMapper::toResponse);
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(productMapper.toResponse(productService.findById(id)));
    }

    @GetMapping("/slug/{slug}")
    public ResponseEntity<ProductResponse> getBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(productMapper.toResponse(productService.findBySlug(slug)));
    }

    // ── ADMIN: full product management ──────────────────────────

    /** List ALL products including inactive drafts. */
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<ProductResponse>> listAll(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(adminProductService.listAll(pageable));
    }

    /**
     * Create a product. If active=true it appears in the public catalogue
     * immediately. Set active=false to save as a draft first.
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adminProductService.createProduct(request));
    }

    /**
     * Full update. Set active=false to hide from customers, active=true to re-publish.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody ProductRequest request) {
        return ResponseEntity.ok(adminProductService.updateProduct(id, request));
    }

    /** Soft delete — hides from customers, data preserved in DB. */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        adminProductService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}
