package com.ecommerce.multivendor.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class AddressRequest {

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100, message = "Full name must be 2-100 characters")
    private String fullName;

    @NotBlank(message = "Phone number is required")
    @Pattern(
        regexp = "^(\\+92|0)[0-9]{9,10}$",
        message = "Enter a valid Pakistan phone number (e.g. 0311-1234567)"
    )
    private String phoneNumber;

    @NotBlank(message = "Address line 1 is required")
    @Size(min = 5, max = 255, message = "Address must be 5-255 characters")
    private String addressLine1;

    @Size(max = 255, message = "Address line 2 max 255 characters")
    private String addressLine2;

    @NotBlank(message = "City is required")
    @Size(min = 2, max = 100, message = "City name must be 2-100 characters")
    private String city;

    @NotBlank(message = "State/Province is required")
    @Size(min = 2, max = 100)
    private String state;

    @NotBlank(message = "Postal code is required")
    @Pattern(regexp = "^[0-9]{5}$", message = "Postal code must be 5 digits")
    private String pincode;

    @Size(max = 100)
    private String country = "Pakistan";

    private boolean defaultAddress;
}
