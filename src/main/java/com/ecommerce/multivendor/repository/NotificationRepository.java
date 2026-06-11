package com.ecommerce.multivendor.repository;

import com.ecommerce.multivendor.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Page<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    long countByUserIdAndReadFalse(Long userId);

    @Modifying @Transactional
    @Query("UPDATE Notification n SET n.read = true WHERE n.user.id = :userId")
    void markAllAsRead(@Param("userId") Long userId);
}
