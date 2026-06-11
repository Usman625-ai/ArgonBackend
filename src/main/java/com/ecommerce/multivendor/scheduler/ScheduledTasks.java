package com.ecommerce.multivendor.scheduler;

import com.ecommerce.multivendor.repository.PasswordResetTokenRepository;
import com.ecommerce.multivendor.service.impl.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScheduledTasks {

    private final OrderService orderService;
    private final PasswordResetTokenRepository passwordResetTokenRepository;

    @Value("${app.order.cancellation-window-hours:24}")
    private int cancellationWindowHours;

    /**
     * Auto-cancel COD orders that haven't been confirmed within 24 hours.
     * Runs every hour.
     */
    @Scheduled(cron = "0 0 * * * *")   // top of every hour
    public void autoCancelStaleOrders() {
        log.info("[Scheduler] Running auto-cancel check at {}",
            LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        LocalDateTime cutoff = LocalDateTime.now().minusHours(cancellationWindowHours);
        try {
            orderService.autoCancelPendingOrders(cutoff);
        } catch (Exception e) {
            log.error("[Scheduler] Auto-cancel failed: {}", e.getMessage(), e);
        }
    }

    /**
     * Clean up expired and used password-reset tokens nightly at 2 AM.
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void cleanupExpiredTokens() {
        log.info("[Scheduler] Cleaning up expired password reset tokens");
        try {
            passwordResetTokenRepository.deleteExpiredTokens(LocalDateTime.now());
            log.info("[Scheduler] Expired tokens cleaned up");
        } catch (Exception e) {
            log.error("[Scheduler] Token cleanup failed: {}", e.getMessage(), e);
        }
    }

    /**
     * Log a daily platform health summary every morning at 7 AM.
     * Extend this to send email or Slack notifications as needed.
     */
    @Scheduled(cron = "0 0 7 * * *")
    public void dailyHealthCheck() {
        log.info("[Scheduler] Daily health check at {}",
            LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
    }
}
