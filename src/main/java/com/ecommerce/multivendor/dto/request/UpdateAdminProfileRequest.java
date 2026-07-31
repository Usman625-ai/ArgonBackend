package com.ecommerce.multivendor.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class UpdateAdminProfileRequest {

    @Size(min = 2, max = 100, message = "Name must be 2-100 characters")
    private String name;

    @Pattern(regexp = "^(\\+92|0)[0-9]{9,10}$", message = "Invalid Pakistan phone number")
    private String contactNumber;

    @Size(max = 500)
    private String profileImage;
}
