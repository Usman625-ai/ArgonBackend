package com.ecommerce.multivendor.repository;

import com.ecommerce.multivendor.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>,
        JpaSpecificationExecutor<Product> {

    @Modifying
    @Query("UPDATE Product p SET p.active = :active WHERE p.id = :id")
    int updateProductStatus(@Param("id") Long id, @Param("active") Boolean active);

    @Query("SELECT p FROM Product p WHERE p.active = true AND p.featured = true ORDER BY p.featuredAt DESC")
    List<Product> findFeaturedProducts();

    @Query("SELECT p FROM Product p WHERE p.active = true AND p.featured = true ORDER BY p.featuredAt DESC")
    Page<Product> findFeaturedProducts(Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.active = true AND p.featured = true AND p.category.id = :categoryId")
    List<Product> findFeaturedProductsByCategory(@Param("categoryId") Long categoryId);

    Optional<Product> findBySlug(String slug);
    boolean existsBySlug(String slug);

    Page<Product> findBySellerId(Long sellerId, Pageable pageable);

    Page<Product> findBySellerIdAndActiveTrue(Long sellerId, Pageable pageable);
    Page<Product> findByCategoryIdAndActiveTrue(Long categoryId, Pageable pageable);
    Page<Product> findByActiveTrue(Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.active = true AND p.seller.active = true AND " +
            "(LOWER(p.name) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
            " LOWER(p.description) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
            " LOWER(p.brand) LIKE LOWER(CONCAT('%',:q,'%')))")
    Page<Product> searchProducts(@Param("q") String q, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.active = true AND p.seller.active = true AND " +
            "(:categoryId IS NULL OR p.category.id = :categoryId " +
            "  OR p.category.parent.id = :categoryId " +
            "  OR p.category.parent.parent.id = :categoryId) AND " +
            "(:minPrice IS NULL OR p.price >= :minPrice) AND " +
            "(:maxPrice IS NULL OR p.price <= :maxPrice) AND " +
            "(:brand IS NULL OR LOWER(p.brand) = LOWER(:brand)) AND " +
            "(:q IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%',:q,'%')))")
    Page<Product> filterProducts(@Param("categoryId") Long categoryId,
                                 @Param("minPrice") BigDecimal minPrice,
                                 @Param("maxPrice") BigDecimal maxPrice,
                                 @Param("brand") String brand,
                                 @Param("q") String q,
                                 Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.active = true AND p.stockQuantity <= :threshold")
    List<Product> findLowStockProducts(@Param("threshold") int threshold);

    @Modifying @Transactional
    @Query("UPDATE Product p SET p.averageRating = :rating, p.totalReviews = :count WHERE p.id = :id")
    void updateRatingStats(@Param("id") Long id,
                           @Param("rating") BigDecimal rating,
                           @Param("count") int count);

    // Seller stats
    @Query("SELECT COUNT(p) FROM Product p WHERE p.seller.id = :sellerId AND p.active = true")
    long countActiveProductsBySeller(@Param("sellerId") Long sellerId);

    @Query("SELECT COUNT(p) FROM Product p WHERE p.seller.id = :sellerId " +
            "AND p.active = true AND p.stockQuantity > 0 AND p.stockQuantity <= :threshold")
    long countLowStockProductsBySeller(@Param("sellerId") Long sellerId,
                                       @Param("threshold") int threshold);

    @Query("SELECT COUNT(p) FROM Product p WHERE p.seller.id = :sellerId " +
            "AND p.active = true AND p.stockQuantity = 0")
    long countOutOfStockProductsBySeller(@Param("sellerId") Long sellerId);

    // Platform stats
    long countByActiveTrue();

    @Query("SELECT DISTINCT p.brand FROM Product p WHERE p.brand IS NOT NULL AND p.active = true ORDER BY p.brand")
    List<String> findAllBrands();
}