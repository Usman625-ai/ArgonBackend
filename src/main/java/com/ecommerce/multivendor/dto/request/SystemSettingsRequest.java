package com.ecommerce.multivendor.dto.request;

import lombok.*;

/**
 * All fields are Optional-style (nullable Boolean) so the caller can do
 * partial updates — e.g. toggle only maintenanceMode without touching
 * allowSellerRegistration.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemSettingsRequest {
    private Boolean maintenanceMode;
    private Boolean allowSellerRegistration;
}
