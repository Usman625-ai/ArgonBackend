package com.ecommerce.multivendor.service.impl;

import com.ecommerce.multivendor.dto.response.PagedResponse;
import com.ecommerce.multivendor.entity.Notification;
import com.ecommerce.multivendor.entity.User;
import com.ecommerce.multivendor.enums.NotificationType;
import com.ecommerce.multivendor.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Async("asyncExecutor")
    public void createNotification(User user, String message, String title,
                                    NotificationType type, String actionUrl) {
        try {
            Notification notification = Notification.builder()
                .user(user)
                .message(message)
                .title(title)
                .type(type)
                .actionUrl(actionUrl)
                .read(false)
                .build();
            notificationRepository.save(notification);
        } catch (Exception e) {
            log.error("Failed to create notification for user {}: {}", user.getId(), e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public PagedResponse<Map<String, Object>> getNotifications(Long userId, int page, int size) {
        Page<Notification> notifPage = notificationRepository.findByUserIdOrderByCreatedAtDesc(
            userId, PageRequest.of(page, size)
        );

        List<Map<String, Object>> content = notifPage.getContent().stream()
            .map(n -> Map.<String, Object>of(
                "id", n.getId(),
                "title", n.getTitle(),
                "message", n.getMessage(),
                "type", n.getType(),
                "read", n.isRead(),
                "actionUrl", n.getActionUrl() != null ? n.getActionUrl() : "",
                "createdAt", n.getCreatedAt()
            ))
            .toList();

        return PagedResponse.<Map<String, Object>>builder()
            .content(content)
            .pageNumber(notifPage.getNumber())
            .pageSize(notifPage.getSize())
            .totalElements(notifPage.getTotalElements())
            .totalPages(notifPage.getTotalPages())
            .last(notifPage.isLast())
            .first(notifPage.isFirst())
            .build();
    }

    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsRead(userId);
    }

    @Transactional
    public void markAsRead(Long userId, Long notificationId) {
        int updated = notificationRepository.markAsRead(notificationId, userId);
        if (updated == 0) {
            throw new com.ecommerce.multivendor.exception.ResourceNotFoundException("Notification", notificationId);
        }
    }
}
