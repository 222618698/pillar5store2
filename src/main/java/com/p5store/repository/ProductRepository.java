package com.p5store.repository;

import com.p5store.domain.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {

    Optional<Product> findBySlug(String slug);

    Page<Product> findByIsActiveTrueAndCategoryId(UUID categoryId, Pageable pageable);

    Page<Product> findByIsActiveTrue(Pageable pageable);

    @Query("""
        SELECT p FROM Product p
        WHERE p.isActive = true
          AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :q, '%'))
            OR LOWER(p.description) LIKE LOWER(CONCAT('%', :q, '%')))
        """)
    Page<Product> search(@Param("q") String query, Pageable pageable);

    boolean existsBySku(String sku);
}
