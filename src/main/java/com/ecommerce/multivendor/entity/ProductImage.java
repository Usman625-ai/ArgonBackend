package com.ecommerce.multivendor.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "product_images")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ProductImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "image_url", nullable = false)
    private String imageUrl;

    @Column(name = "public_id")
    private String publicId;  // Cloudinary public ID for deletion

    @Column(name = "is_primary")
    @Builder.Default
    private boolean primary = false;

    @Column(name = "display_order")
    @Builder.Default
    private int displayOrder = 0;
}
