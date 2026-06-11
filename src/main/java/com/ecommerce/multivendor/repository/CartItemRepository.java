package com.ecommerce.multivendor.repository;

import com.ecommerce.multivendor.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    List<CartItem> findByUserId(Long userId);
    Optional<CartItem> findByUserIdAndProductId(Long userId, Long productId);
    void deleteByUserId(Long userId);
    int countByUserId(Long userId);

    @Query("SELECT SUM(ci.quantity * p.price) FROM CartItem ci JOIN ci.product p WHERE ci.user.id = :userId")
    BigDecimal calculateCartTotal(@Param("userId") Long userId);
}
