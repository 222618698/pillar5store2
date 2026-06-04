package com.p5store.service;

import com.p5store.domain.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ProductService {
    Product findById(UUID id);
    Product findBySlug(String slug);
    Page<Product> findAll(Pageable pageable);
    Page<Product> findByCategory(UUID categoryId, Pageable pageable);
    Page<Product> search(String query, Pageable pageable);
    Product create(Product product);
    Product update(UUID id, Product updated);
    void delete(UUID id);
}