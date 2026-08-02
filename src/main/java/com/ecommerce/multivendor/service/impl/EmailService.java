package com.ecommerce.multivendor.service.impl;

import com.ecommerce.multivendor.entity.Order;
import com.ecommerce.multivendor.entity.User;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromEmail;

    @Value("${app.mail.from-name}")
    private String fromName;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    // ─── Core send method ─────────────────────────────────────────────────

    @Async("asyncExecutor")
    public void sendEmail(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("Email sent to: {} | Subject: {}", to, subject);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }

    // ─── Password Reset ────────────────────────────────────────────────────

    public void sendPasswordResetEmail(User user, String token) {
        String resetLink = frontendUrl + "/reset-password?token=" + token;
        String subject = "Reset Your Password - " + fromName;
        String body = buildEmailTemplate(
                "Password Reset Request",
                "Hi " + user.getName() + ",",
                "Click the button below to reset your password:",
                null,
                "This link will expire in 24 hours. If you didn't request this, ignore this email.",
                resetLink, "Reset Password"
        );
        sendEmail(user.getEmail(), subject, body);
    }

    // ─── Order Confirmation ────────────────────────────────────────────────

    public void sendOrderConfirmation(Order order) {
        String subject = "Order Confirmed #" + order.getOrderNumber();
        String content = buildOrderSummaryHtml(order);
        String body = buildEmailTemplate(
                "Order Confirmed! 🎉",
                "Hi " + order.getCustomer().getName() + ",",
                "Your order has been placed successfully.",
                content,
                "We'll notify you when your order is shipped.",
                frontendUrl + "/orders/" + order.getId(), "Track Order"
        );
        sendEmail(order.getCustomer().getEmail(), subject, body);
    }

    // ─── Order Shipped ─────────────────────────────────────────────────────

    public void sendOrderShipped(Order order) {
        String subject = "Your Order is on the Way! 🚚 #" + order.getOrderNumber();
        String body = buildEmailTemplate(
                "Order Shipped!",
                "Hi " + order.getCustomer().getName() + ",",
                "Great news! Your order has been shipped.",
                order.getTrackingNumber() != null
                        ? "<p>Tracking Number: <strong>" + order.getTrackingNumber() + "</strong></p>"
                        : null,
                "Estimated delivery: " + (order.getEstimatedDeliveryDate() != null
                        ? order.getEstimatedDeliveryDate().toLocalDate().toString()
                        : "3-5 business days"),
                frontendUrl + "/orders/" + order.getId(), "Track Order"
        );
        sendEmail(order.getCustomer().getEmail(), subject, body);
    }

    // ─── Order Delivered ───────────────────────────────────────────────────

    public void sendOrderDelivered(Order order) {
        String subject = "Order Delivered! #" + order.getOrderNumber();
        String body = buildEmailTemplate(
                "Order Delivered! ✅",
                "Hi " + order.getCustomer().getName() + ",",
                "Your order has been delivered successfully. We hope you love it!",
                null,
                "Please leave a review to help other shoppers.",
                frontendUrl + "/orders/" + order.getId(), "Leave a Review"
        );
        sendEmail(order.getCustomer().getEmail(), subject, body);
    }

    // ─── Order Cancelled ───────────────────────────────────────────────────

    public void sendOrderCancelled(Order order, String reason) {
        String subject = "Order Cancelled #" + order.getOrderNumber();
        String body = buildEmailTemplate(
                "Order Cancelled",
                "Hi " + order.getCustomer().getName() + ",",
                "Your order #" + order.getOrderNumber() + " has been cancelled.",
                reason != null ? "<p>Reason: " + reason + "</p>" : null,
                "If payment was made online, the refund will be processed within 5-7 business days.",
                frontendUrl + "/products", "Continue Shopping"
        );
        sendEmail(order.getCustomer().getEmail(), subject, body);
    }

    // ─── Seller Approval ───────────────────────────────────────────────────

    public void sendSellerApprovalEmail(User seller) {
        String subject = "Seller Account Approved! - " + fromName;
        String body = buildEmailTemplate(
                "Welcome to " + fromName + "! 🎉",
                "Hi " + seller.getName() + ",",
                "Congratulations! Your seller account has been approved.",
                "<p>Your shop <strong>" + seller.getShopName() + "</strong> is now live!</p>",
                "You can now start adding products and selling to thousands of customers.",
                frontendUrl + "/seller/dashboard", "Go to Seller Dashboard"
        );
        sendEmail(seller.getEmail(), subject, body);
    }

    public void sendSellerRejectionEmail(User seller, String reason) {
        String subject = "Seller Registration Update - " + fromName;
        String body = buildEmailTemplate(
                "Seller Registration Update",
                "Hi " + seller.getName() + ",",
                "Unfortunately, your seller registration could not be approved at this time.",
                reason != null ? "<p>Reason: " + reason + "</p>" : null,
                "You may re-apply after addressing the mentioned concerns.",
                frontendUrl + "/seller/register", "Re-Apply"
        );
        sendEmail(seller.getEmail(), subject, body);
    }

    // ─── New Order for Seller ─────────────────────────────────────────────

    public void sendNewOrderNotificationToSeller(Order order) {
        String subject = "New Order Received #" + order.getOrderNumber();
        String body = buildEmailTemplate(
                "New Order Alert! 📦",
                "Hi " + order.getSeller().getName() + ",",
                "You have received a new order.",
                buildOrderSummaryHtml(order),
                "Please process this order promptly.",
                frontendUrl + "/seller/orders/" + order.getId(), "View Order"
        );
        sendEmail(order.getSeller().getEmail(), subject, body);
    }

    // ─── Payment Success ───────────────────────────────────────────────────

    public void sendPaymentSuccess(Order order) {
        String subject = "Payment Successful - #" + order.getOrderNumber();
        String body = buildEmailTemplate(
                "Payment Successful! 💳",
                "Hi " + order.getCustomer().getName() + ",",
                "Payment of ₹" + order.getFinalAmount() + " received for order #" + order.getOrderNumber(),
                null,
                "Your order is being processed.",
                frontendUrl + "/orders/" + order.getId(), "View Order"
        );
        sendEmail(order.getCustomer().getEmail(), subject, body);
    }

    // ─── HTML Template Builder ─────────────────────────────────────────────

    private String buildEmailTemplate(String title, String greeting, String message,
                                      String extraContent, String footer,
                                      String ctaLink, String ctaText) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><head><meta charset='UTF-8'>");
        sb.append("<style>body{font-family:Arial,sans-serif;background:#f5f5f5;margin:0;padding:0;}");
        sb.append(".container{max-width:600px;margin:40px auto;background:#fff;border-radius:8px;");
        sb.append("box-shadow:0 2px 8px rgba(0,0,0,0.1);overflow:hidden;}");
        sb.append(".header{background:linear-gradient(135deg,#6366f1,#8b5cf6);padding:30px;text-align:center;}");
        sb.append(".header h1{color:#fff;margin:0;font-size:24px;}");
        sb.append(".body{padding:30px;color:#333;}");
        sb.append(".body h2{color:#6366f1;}");
        sb.append(".cta{display:inline-block;background:#6366f1;color:#fff;text-decoration:none;");
        sb.append("padding:12px 30px;border-radius:6px;margin:20px 0;font-weight:bold;}");
        sb.append(".footer{background:#f5f5f5;padding:20px;text-align:center;color:#999;font-size:12px;}");
        sb.append("</style></head><body><div class='container'>");
        sb.append("<div class='header'><h1>").append(fromName).append("</h1></div>");
        sb.append("<div class='body'>");
        sb.append("<h2>").append(title).append("</h2>");
        if (greeting != null) sb.append("<p>").append(greeting).append("</p>");
        sb.append("<p>").append(message).append("</p>");
        if (extraContent != null) sb.append(extraContent);
        if (ctaLink != null && ctaText != null) {
            sb.append("<div style='text-align:center;'>");
            sb.append("<a href='").append(ctaLink).append("' class='cta'>").append(ctaText).append("</a>");
            sb.append("</div>");
        }
        if (footer != null) sb.append("<p style='color:#666;font-size:13px;'>").append(footer).append("</p>");
        sb.append("</div><div class='footer'>© 2024 ").append(fromName);
        sb.append(". All rights reserved.</div></div></body></html>");
        return sb.toString();
    }

    private String buildOrderSummaryHtml(Order order) {
        StringBuilder sb = new StringBuilder();
        sb.append("<table style='width:100%;border-collapse:collapse;margin:15px 0;'>");
        sb.append("<tr style='background:#f5f5f5;'>");
        sb.append("<th style='padding:8px;text-align:left;border:1px solid #ddd;'>Product</th>");
        sb.append("<th style='padding:8px;text-align:center;border:1px solid #ddd;'>Qty</th>");
        sb.append("<th style='padding:8px;text-align:right;border:1px solid #ddd;'>Price</th>");
        sb.append("</tr>");
        order.getOrderItems().forEach(item -> {
            sb.append("<tr>");
            sb.append("<td style='padding:8px;border:1px solid #ddd;'>").append(item.getProductName()).append("</td>");
            sb.append("<td style='padding:8px;text-align:center;border:1px solid #ddd;'>").append(item.getQuantity()).append("</td>");
            sb.append("<td style='padding:8px;text-align:right;border:1px solid #ddd;'>₹").append(item.getTotalPrice()).append("</td>");
            sb.append("</tr>");
        });
        sb.append("<tr><td colspan='2' style='padding:8px;text-align:right;border:1px solid #ddd;'><strong>Total</strong></td>");
        sb.append("<td style='padding:8px;text-align:right;border:1px solid #ddd;'><strong>₹").append(order.getFinalAmount()).append("</strong></td></tr>");
        sb.append("</table>");
        return sb.toString();
    }

    // ─── Email Verification OTP ────────────────────────────────────────────

    public void sendVerificationOtpEmail(String email, String name, String otp) {
        String subject = "Verify Your Email - " + fromName;
        String body = buildEmailTemplate(
                "Email Verification",
                "Hi " + name + ",",
                "Your email verification OTP is:",
                "<h1 style='color:#6366f1;letter-spacing:8px;text-align:center;'>" + otp + "</h1>",
                "This OTP will expire in 1 minute. Do not share it with anyone. If you did not request this, please ignore this email.",
                null, null
        );
        sendEmail(email, subject, body);
        log.info("Verification OTP email sent to: {}", email);
    }

    // ─── Change Password OTP ───────────────────────────────────────────────

    public void sendChangePasswordOtpEmail(String email, String name, String otp) {
        String subject = "Change Password Request - " + fromName;
        String body = buildEmailTemplate(
                "Change Password Request",
                "Hi " + name + ",",
                "Your OTP to change your account password is:",
                "<h1 style='color:#6366f1;letter-spacing:8px;text-align:center;'>" + otp + "</h1>",
                "This OTP will expire in 1 minute. If you did not request this, please secure your account and ignore this email.",
                null, null
        );
        sendEmail(email, subject, body);
        log.info("Change-password OTP email sent to: {}", email);
    }

}