package com.ecommerce.multivendor.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "products", indexes = {
    @Index(name = "idx_product_slug", columnList = "slug"),
    @Index(name = "idx_product_seller", columnList = "seller_id"),
    @Index(name = "idx_product_category", columnList = "category_id"),
    @Index(name = "idx_product_active", columnList = "is_active")
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Product name is required")
    @Size(min = 2, max = 200, message = "Product name must be 2-200 characters")
    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String slug;

    @NotBlank(message = "Description is required")
    @Size(min = 10, message = "Description must be at least 10 characters")
    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(name = "short_description")
    private String shortDescription;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "discounted_price", precision = 10, scale = 2)
    private BigDecimal discountedPrice;

    @Min(value = 0, message = "Stock cannot be negative")
    @Column(name = "stock_quantity", nullable = false)
    @Builder.Default
    private int stockQuantity = 0;

    private String brand;

    // Stored as JSON string: ["tag1", "tag2"]
    @Column(columnDefinition = "JSON")
    private String tags;

    // Stored as JSON: {"color": "Red", "size": "XL"}
    @Column(columnDefinition = "JSON")
    private String specifications;

    @Column(name = "average_rating", precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal averageRating = BigDecimal.ZERO;

    @Column(name = "total_reviews")
    @Builder.Default
    private int totalReviews = 0;

    @Column(name = "total_sold")
    @Builder.Default
    private int totalSold = 0;

    @Column(name = "is_active")
    @Builder.Default
    private boolean active = true;

    @Column(name = "is_featured")
    @Builder.Default
    private boolean featured = false;

    // Relationships

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ProductImage> images = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Review> reviews = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<CartItem> cartItems = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Wishlist> wishlistItems = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Helper methods

    public BigDecimal getEffectivePrice() {
        return (discountedPrice != null && discountedPrice.compareTo(BigDecimal.ZERO) > 0)
            ? discountedPrice : price;
    }

    public boolean isInStock() {
        return stockQuantity > 0;
    }

    public void decreaseStock(int quantity) {
        if (this.stockQuantity < quantity) {
            throw new IllegalStateException("Insufficient stock for product: " + name);
        }
        this.stockQuantity -= quantity;
        this.totalSold += quantity;
    }

    public void increaseStock(int quantity) {
        this.stockQuantity += quantity;
    }
}
