package com.p5store.service.impl;

import com.p5store.domain.Product;
import com.p5store.exception.DuplicateResourceException;
import com.p5store.exception.ResourceNotFoundException;
import com.p5store.repository.ProductRepository;
import com.p5store.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    @Override
    public Product findById(UUID id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
    }

    @Override
    public Product findBySlug(String slug) {
        return productRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with slug: " + slug));
    }

    @Override
    public Page<Product> findAll(Pageable pageable) {
        return productRepository.findByIsActiveTrue(pageable);
    }

    @Override
    public Page<Product> findByCategory(UUID categoryId, Pageable pageable) {
        return productRepository.findByIsActiveTrueAndCategoryId(categoryId, pageable);
    }

    @Override
    public Page<Product> search(String query, Pageable pageable) {
        return productRepository.search(query.trim(), pageable);
    }

    @Override
    @Transactional
    public Product create(Product product) {
        if (productRepository.existsBySku(product.getSku())) {
            throw new DuplicateResourceException("SKU already exists: " + product.getSku());
        }
        return productRepository.save(product);
    }

    @Override
    @Transactional
    public Product update(UUID id, Product updated) {
        Product existing = findById(id);
        existing.setName(updated.getName());
        existing.setDescription(updated.getDescription());
        existing.setBasePrice(updated.getBasePrice());
        existing.setCategory(updated.getCategory());
        existing.setActive(updated.isActive());
        return productRepository.save(existing);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        Product product = findById(id);
        product.setActive(false);          // soft delete
        productRepository.save(product);
    }
}