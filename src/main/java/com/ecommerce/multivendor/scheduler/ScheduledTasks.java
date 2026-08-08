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

    @Value("${app.order.jazzcash-payment-window-minutes}")
    private int paymentWindowMinutes;

    /**
     * Auto-cancel JazzCash orders that haven't been paid within the payment window
     * (default 10 minutes). COD orders are never auto-cancelled — see
     * OrderService#autoCancelPendingOrders.
     *
     * Runs every 30 minutes rather than every minute — this means a stale order can
     * sit up to ~30 minutes past its 10-minute window before cleanup (stock stays
     * reserved a little longer for an abandoned/unpaid cart), but that's a fine
     * trade for far fewer scheduled DB round trips in production. Tighten the cron
     * below if you ever need faster cleanup.
     */
    @Scheduled(cron = "${app.order.auto-cancel-cron:0 0,30 * * * *}")
    public void autoCancelStaleOrders() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(paymentWindowMinutes);
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