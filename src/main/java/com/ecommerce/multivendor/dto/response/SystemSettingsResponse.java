package com.ecommerce.multivendor.dto.response;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemSettingsResponse {
    private boolean maintenanceMode;
    private boolean allowSellerRegistration;
}
