package com.p5store.repository;

import com.p5store.domain.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {
    Page<Review> findByProductId(UUID productId, Pageable pageable);
    boolean existsByProductIdAndUserId(UUID productId, UUID userId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.product.id = :productId")
    Optional<Double> findAverageRatingByProductId(@Param("productId") UUID productId);
}
