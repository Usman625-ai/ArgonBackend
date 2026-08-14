package com.ecommerce.multivendor.repository;

import com.ecommerce.multivendor.entity.Order;
import com.ecommerce.multivendor.enums.OrderStatus;
import com.ecommerce.multivendor.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByOrderNumber(String orderNumber);

    /**
     * Overrides the default findById to also eagerly fetch customer+seller.
     * This matters beyond just avoiding N+1: several email-sending methods
     * (sendOrderConfirmation, sendOrderShipped, etc.) are @Async and read
     * order.getCustomer()/getSeller() — if those were still lazy proxies, the
     * async thread could hit them after the original request's Hibernate
     * session already closed and throw LazyInitializationException. Eager
     * loading here means that data is always already resolved, safe from any
     * thread, at any time.
     */
    @EntityGraph(attributePaths = {"customer", "seller"})
    Optional<Order> findById(Long id);

    // Customer queries
    @EntityGraph(attributePaths = {"customer", "seller"})
    Page<Order> findByCustomerIdOrderByCreatedAtDesc(Long customerId, Pageable pageable);

    @EntityGraph(attributePaths = {"customer", "seller"})
    Page<Order> findByCustomerIdAndOrderStatusOrderByCreatedAtDesc(Long customerId, OrderStatus orderStatus, Pageable pageable);

    // Seller queries
    @EntityGraph(attributePaths = {"customer", "seller"})
    Page<Order> findBySellerIdOrderByCreatedAtDesc(Long sellerId, Pageable pageable);

    // Admin queries
    @EntityGraph(attributePaths = {"customer", "seller"})
    Page<Order> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // Coupon per-user usage count (excludes cancelled orders)
    @Query("SELECT COUNT(o) FROM Order o WHERE o.customer.id = :customerId " +
            "AND o.couponCode = :couponCode AND o.orderStatus != 'CANCELLED'")
    long countByCustomerIdAndCouponCode(@Param("customerId") Long customerId, @Param("couponCode") String couponCode);

    // Stats - platform-wide
    @Query("SELECT COUNT(o) FROM Order o WHERE o.orderStatus != 'CANCELLED'")
    long countActiveOrders();

    /** Platform-wide pending orders (PENDING or CONFIRMED) — mirrors countPendingOrdersBySeller */
    @Query("SELECT COUNT(o) FROM Order o WHERE o.orderStatus IN ('PENDING','CONFIRMED')")
    long countPendingOrders();

    @Query("SELECT COALESCE(SUM(o.finalAmount), 0) FROM Order o WHERE o.paymentStatus = 'PAID'")
    BigDecimal calculateTotalRevenue();

    @Query("SELECT COALESCE(SUM(o.finalAmount), 0) FROM Order o WHERE o.paymentStatus = 'PAID' " +
            "AND o.createdAt BETWEEN :from AND :to")
    BigDecimal calculateRevenueByDateRange(@Param("from") LocalDateTime from,
                                           @Param("to") LocalDateTime to);

    // Seller stats
    @Query("SELECT COUNT(o) FROM Order o WHERE o.seller.id = :sellerId AND o.orderStatus != 'CANCELLED'")
    long countActiveOrdersBySeller(@Param("sellerId") Long sellerId);

    @Query("SELECT COALESCE(SUM(o.finalAmount), 0) FROM Order o WHERE o.seller.id = :sellerId " +
            "AND o.paymentStatus = 'PAID'")
    BigDecimal calculateRevenueForSeller(@Param("sellerId") Long sellerId);

    /** Count orders for seller grouped by each status */
    @Query("SELECT o.orderStatus, COUNT(o) FROM Order o WHERE o.seller.id = :sellerId " +
            "GROUP BY o.orderStatus")
    List<Object[]> countOrdersByStatusForSeller(@Param("sellerId") Long sellerId);

    /** Pending orders (PENDING or CONFIRMED) awaiting seller action */
    @Query("SELECT COUNT(o) FROM Order o WHERE o.seller.id = :sellerId " +
            "AND o.orderStatus IN ('PENDING','CONFIRMED')")
    long countPendingOrdersBySeller(@Param("sellerId") Long sellerId);

    /** Active in-progress orders (PROCESSING, SHIPPED, OUT_FOR_DELIVERY) */
    @Query("SELECT COUNT(o) FROM Order o WHERE o.seller.id = :sellerId " +
            "AND o.orderStatus IN ('PROCESSING','SHIPPED','OUT_FOR_DELIVERY')")
    long countActiveInProgressOrdersBySeller(@Param("sellerId") Long sellerId);

    /** Delivered orders for seller */
    @Query("SELECT COUNT(o) FROM Order o WHERE o.seller.id = :sellerId " +
            "AND o.orderStatus = 'DELIVERED'")
    long countDeliveredOrdersBySeller(@Param("sellerId") Long sellerId);

    /** Cancelled orders for seller */
    @Query("SELECT COUNT(o) FROM Order o WHERE o.seller.id = :sellerId " +
            "AND o.orderStatus = 'CANCELLED'")
    long countCancelledOrdersBySeller(@Param("sellerId") Long sellerId);

    /** Monthly revenue for seller */
    @Query("SELECT COALESCE(SUM(o.finalAmount), 0) FROM Order o WHERE o.seller.id = :sellerId " +
            "AND o.paymentStatus = 'PAID' AND o.createdAt BETWEEN :from AND :to")
    BigDecimal calculateRevenueForSellerByDateRange(@Param("sellerId") Long sellerId,
                                                    @Param("from") LocalDateTime from,
                                                    @Param("to") LocalDateTime to);

    /** Daily revenue for seller (last 7 days) */
    @Query("SELECT DATE(o.createdAt) as date, COALESCE(SUM(o.finalAmount), 0) as revenue " +
            "FROM Order o WHERE o.seller.id = :sellerId AND o.paymentStatus = 'PAID' " +
            "AND o.createdAt >= :since GROUP BY DATE(o.createdAt) ORDER BY date ASC")
    List<Object[]> getDailyRevenueForSeller(@Param("sellerId") Long sellerId,
                                            @Param("since") LocalDateTime since);

    // Auto-cancel: find pending, unpaid JazzCash orders older than given time.
    // COD orders are never auto-cancelled — they stay PENDING until a seller
    // confirms or the customer cancels, since no online payment is expected.
    @Query("SELECT o FROM Order o WHERE o.orderStatus = 'PENDING' AND o.createdAt < :cutoff " +
            "AND o.paymentMethod = 'JAZZCASH' AND o.paymentStatus <> 'PAID'")
    List<Order> findPendingOrdersOlderThan(@Param("cutoff") LocalDateTime cutoff);

    // Reports
    @Query("SELECT o FROM Order o WHERE o.seller.id = :sellerId AND o.createdAt BETWEEN :from AND :to " +
            "ORDER BY o.createdAt DESC")
    List<Order> findBySellerAndDateRange(@Param("sellerId") Long sellerId,
                                         @Param("from") LocalDateTime from,
                                         @Param("to") LocalDateTime to);

    @Query("SELECT o FROM Order o WHERE o.createdAt BETWEEN :from AND :to ORDER BY o.createdAt DESC")
    List<Order> findByDateRange(@Param("from") LocalDateTime from,
                                @Param("to") LocalDateTime to);

    // Dashboard chart data: daily revenue for last N days
    @Query("SELECT DATE(o.createdAt) as date, COALESCE(SUM(o.finalAmount), 0) as revenue " +
            "FROM Order o WHERE o.paymentStatus = 'PAID' AND o.createdAt >= :since " +
            "GROUP BY DATE(o.createdAt) ORDER BY date ASC")
    List<Object[]> getDailyRevenue(@Param("since") LocalDateTime since);

    /** Find orders by seller and orderStatus */
    @Query("SELECT o FROM Order o WHERE o.seller.id = :sellerId AND o.orderStatus = :orderStatus")
    List<Order> findBySellerIdAndOrderStatus(@Param("sellerId") Long sellerId, @Param("orderStatus") OrderStatus orderStatus);

    /** Monthly revenue for seller chart data */
    @Query(value = """
    SELECT 
        MONTH(o.created_at) as month,
        SUM(o.final_amount) as revenue,
        COUNT(*) as order_count,
        COALESCE(SUM(oi.quantity), 0) as product_count
    FROM orders o
    LEFT JOIN order_items oi ON o.id = oi.order_id
    WHERE o.seller_id = :sellerId 
    AND YEAR(o.created_at) = :year
    AND o.order_status NOT IN ('CANCELLED', 'REFUNDED')
    GROUP BY MONTH(o.created_at)
    ORDER BY MONTH(o.created_at)
    """, nativeQuery = true)

    List<Object[]> findMonthlyRevenueBySellerAndYear(
            @Param("sellerId") Long sellerId,
            @Param("year") int year);


    long countByOrderNumberStartingWith(String prefix);
}