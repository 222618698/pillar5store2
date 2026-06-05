package com.p5store.service;

import com.p5store.dto.request.ProductRequest;
import com.p5store.dto.response.ProductResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Admin-facing product operations.
 * Results flow through the ProductMapper so customers always get consistent
 * ProductResponse objects — including newly created products.
 */
public interface AdminProductService {
    ProductResponse createProduct(ProductRequest request);
    ProductResponse updateProduct(UUID id, ProductRequest request);
    void deleteProduct(UUID id);                     // soft delete
    Page<ProductResponse> listAll(Pageable pageable); // includes inactive
}
