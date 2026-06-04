package com.p5store.repository;

import com.p5store.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {
    Optional<Category> findBySlug(String slug);
    List<Category> findByParentIsNullAndIsActiveTrue();
    List<Category> findByParentIdAndIsActiveTrue(UUID parentId);
}