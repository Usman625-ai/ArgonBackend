package com.ecommerce.multivendor.repository;

import com.ecommerce.multivendor.entity.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findBySlug(String slug);
    boolean existsBySlug(String slug);
    boolean existsByName(String name);

    // Get direct children of a parent category
    List<Category> findByParentIdAndActiveTrue(Long parentId);

    // Or get all children (including deeper levels – see note below)
    @Query("SELECT c FROM Category c WHERE c.parent.id = :parentId AND c.active = true ORDER BY c.displayOrder ASC")
    List<Category> findActiveSubcategoriesByParentId(@Param("parentId") Long parentId);

    List<Category> findByParentIsNullAndActiveTrue();

    @Query("SELECT c FROM Category c WHERE c.parent IS NULL AND c.active = true ORDER BY c.displayOrder ASC")
    List<Category> findRootCategoriesOrdered();

    @Query("SELECT c FROM Category c WHERE c.active = true AND " +
            "LOWER(c.name) LIKE LOWER(CONCAT('%',:q,'%'))")
    Page<Category> searchByName(@Param("q") String q, Pageable pageable);

    @Query("SELECT COUNT(p) FROM Product p WHERE p.category.id = :categoryId " +
            "OR p.category.parent.id = :categoryId " +
            "OR p.category.parent.parent.id = :categoryId AND p.active = true")
    long countProductsByCategoryId(@Param("categoryId") Long categoryId);
}
