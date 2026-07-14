package com.ecommerce.multivendor.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SiteSettingResponse {
    private Long id;
    private String siteName;
    private String contactEmail;
    private String currencySymbol;
    private String currencyCode;
    private String phoneNumber;
    private String address;
    private String logoUrl;
    private String faviconUrl;
    private String metaDescription;
    private Double taxRate;
    private Double shippingFee;
    private Double freeShippingThreshold;
    private LocalDateTime updatedAt;
}