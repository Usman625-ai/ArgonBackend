package com.ecommerce.multivendor.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "categories", indexes = {
    @Index(name = "idx_category_slug", columnList = "slug"),
    @Index(name = "idx_category_parent", columnList = "parent_id")
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Category name is required")
    @Size(min = 2, max = 100, message = "Name must be 2-100 characters")
    @Column(nullable = false)
    private String name;

    @NotBlank(message = "Slug is required")
    @Size(max = 120)
    @Column(nullable = false, unique = true)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "is_active")
    @Builder.Default
    private boolean active = true;

    @Column(name = "display_order")
    @Builder.Default
    private int displayOrder = 0;

    // ─── Self-referential hierarchy ─────────────────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Category> children = new ArrayList<>();

    @OneToMany(mappedBy = "category", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Product> products = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
