package com.ecommerce.multivendor.repository;

import com.ecommerce.multivendor.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {
    List<ProductImage> findByProductId(Long productId);
    Optional<ProductImage> findByProductIdAndPrimaryTrue(Long productId);
    void deleteByProductId(Long productId);

    /** Batch lookup for a page of products — one query instead of one-per-product. */
    List<ProductImage> findByProductIdIn(List<Long> productIds);
}
