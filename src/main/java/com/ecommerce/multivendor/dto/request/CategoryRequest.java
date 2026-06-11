package com.ecommerce.multivendor.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CategoryRequest {

    @NotBlank(message = "Category name is required")
    @Size(min = 2, max = 100, message = "Name must be 2-100 characters")
    @Pattern(
        regexp = "^[a-zA-Z0-9&'\\s-]+$",
        message = "Category name can only contain letters, numbers, spaces and -&'"
    )
    private String name;

    @Size(max = 500, message = "Description max 500 characters")
    private String description;

    @Size(max = 500, message = "Image URL too long")
    private String imageUrl;

    @Positive(message = "Parent category ID must be positive")
    private Long parentId;

    private boolean active = true;

    @Min(value = 0, message = "Display order must be 0 or more")
    @Max(value = 999, message = "Display order too large")
    private int displayOrder;
}
