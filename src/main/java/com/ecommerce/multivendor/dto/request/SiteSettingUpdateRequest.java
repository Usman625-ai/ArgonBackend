package com.ecommerce.multivendor.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

@Data
public class SiteSettingUpdateRequest {

    @NotBlank(message = "Site name is required")
    private String siteName;

    @NotBlank(message = "Contact email is required")
    @Email(message = "Invalid email format")
    private String contactEmail;

    @NotBlank(message = "Currency symbol is required")
    private String currencySymbol;

    private String currencyCode;

    private String phoneNumber;

    private String address;

    private String logoUrl;

    private String faviconUrl;

    private String metaDescription;

    @PositiveOrZero(message = "Tax rate must be zero or positive")
    private Double taxRate;

    @PositiveOrZero(message = "Shipping fee must be zero or positive")
    private Double shippingFee;

    @PositiveOrZero(message = "Free shipping threshold must be zero or positive")
    private Double freeShippingThreshold;
}