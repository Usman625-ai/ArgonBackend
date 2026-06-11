package com.ecommerce.multivendor.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class UpdateSellerProfileRequest {

    @Size(min = 2, max = 100, message = "Shop name must be 2-100 characters")
    private String shopName;

    @Size(max = 1000, message = "Shop description max 1000 characters")
    private String shopDescription;

    @Size(max = 500)
    private String shopLogo;

    @Size(max = 500)
    private String shopBanner;

    @Pattern(regexp = "^[0-9]{13}$", message = "GST number must be 13 digits")
    private String gstNumber;

    @Pattern(regexp = "^[A-Z]{5}[0-9]{4}[A-Z]$", message = "Invalid PAN number format")
    private String panNumber;

    @Pattern(regexp = "^(\\+92|0)[0-9]{9,10}$", message = "Invalid Pakistan phone number")
    private String contactNumber;

    @Size(max = 25, message = "Bank account number max 25 characters")
    private String bankAccountNumber;

    @Size(max = 20)
    private String bankIfsc;

    @Size(max = 100)
    private String bankName;
}
