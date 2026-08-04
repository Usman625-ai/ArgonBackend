package com.ecommerce.multivendor.repository;

import com.ecommerce.multivendor.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrderId(Long orderId);

    /** Batch lookup for a page of orders — one query instead of one-per-order. */
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"product"})
    List<OrderItem> findByOrderIdIn(List<Long> orderIds);

    @Query("SELECT oi FROM OrderItem oi WHERE oi.order.customer.id = :customerId " +
            "AND oi.product.id = :productId AND oi.order.orderStatus = 'DELIVERED'")
    List<OrderItem> findDeliveredByCustomerAndProduct(@Param("customerId") Long customerId,
                                                      @Param("productId") Long productId);

    boolean existsByProductId(Long productId);
}