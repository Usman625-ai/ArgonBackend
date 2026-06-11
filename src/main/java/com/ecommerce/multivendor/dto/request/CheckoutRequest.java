package com.ecommerce.multivendor.dto.request;

import com.ecommerce.multivendor.enums.PaymentMethod;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CheckoutRequest {

    @NotNull(message = "Delivery address is required")
    @Positive(message = "Address ID must be positive")
    private Long addressId;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    @Size(max = 30, message = "Coupon code too long")
    @Pattern(regexp = "^[A-Z0-9_-]*$", message = "Invalid coupon code format")
    private String couponCode;

    @Size(max = 500, message = "Notes cannot exceed 500 characters")
    private String notes;
}
