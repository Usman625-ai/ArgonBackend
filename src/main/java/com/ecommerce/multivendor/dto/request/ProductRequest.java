package com.ecommerce.multivendor.dto.request;

import com.ecommerce.multivendor.validation.ValidProductPrice;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * Request body for creating or updating a product.
 *
 * Validation rules:
 *  - name         : 2–200 chars, required
 *  - description  : 10–5000 chars, required
 *  - price        : > 0, required
 *  - discountedPrice : > 0 AND < price (cross-field via @ValidProductPrice)
 *  - stockQuantity : 0–999999, required
 *  - categoryId   : must exist AND be active — enforced in ProductService
 */
@Data
@ValidProductPrice          // ← cross-field: discountedPrice < price
public class ProductRequest {

    @NotBlank(message = "Product name is required")
    @Size(min = 2, max = 200, message = "Product name must be 2–200 characters")
    private String name;

    @NotBlank(message = "Product description is required")
    @Size(min = 10, max = 5000, message = "Description must be 10–5000 characters")
    private String description;

    @Size(max = 300, message = "Short description must not exceed 300 characters")
    private String shortDescription;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    @DecimalMax(value = "9999999.99", message = "Price cannot exceed Rs. 9,999,999")
    private BigDecimal price;

    // discountedPrice vs price validated by @ValidProductPrice at class level
    @DecimalMin(value = "0.01", message = "Discounted price must be greater than 0")
    private BigDecimal discountedPrice;

    @NotNull(message = "Stock quantity is required")
    @Min(value = 0, message = "Stock quantity cannot be negative")
    @Max(value = 999999, message = "Stock quantity cannot exceed 999,999")
    private Integer stockQuantity;

    @Size(max = 100, message = "Brand name must not exceed 100 characters")
    private String brand;

    /**
     * MUST reference an existing, active Category.
     * Service layer validates: category exists, is active, and is not soft-deleted.
     */
    @NotNull(message = "Category is required")
    @Positive(message = "Category ID must be a positive number")
    private Long categoryId;

    // JSON string e.g. ["tag1","tag2"] — validated for length only
    @Size(max = 500, message = "Tags string is too long")
    private String tags;

    // JSON string e.g. {"Key":"Value"} — validated for length only
    @Size(max = 5000, message = "Specifications string is too long")
    private String specifications;

    private boolean featured;

    @Size(max = 500, message = "Image URL is too long")
    private String PrimaryImageUrl;

    private List<@Size(max = 500, message = "Image URL is too long") String> imageUrls;
}