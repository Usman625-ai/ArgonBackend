package com.ecommerce.multivendor.dto.request;

import com.ecommerce.multivendor.enums.Role;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    @Size(max = 150, message = "Email cannot exceed 150 characters")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 50, message = "Password must be 8-50 characters")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$",
        message = "Password must contain at least one uppercase, one lowercase, and one digit"
    )
    private String password;

    @NotNull(message = "Role is required")
    private Role role;

    @Size(min = 2, max = 100, message = "Shop name must be between 2 and 100 characters")
    private String shopName;

    @Pattern(
        regexp = "^(\\+92|0)[0-9]{9,10}$",
        message = "Enter a valid Pakistan phone number (e.g. 0311-1234567)"
    )
    private String contactNumber;
}
