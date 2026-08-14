package com.ecommerce.multivendor.controller;

import com.ecommerce.multivendor.dto.response.*;
import com.ecommerce.multivendor.service.impl.CategoryService;
import com.ecommerce.multivendor.service.impl.ProductService;
import com.ecommerce.multivendor.service.impl.ReviewService;
import com.ecommerce.multivendor.service.impl.SellerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Fully public endpoints — no JWT required.
 * Used by the storefront for browsing, search, and SEO.
 */
@RestController
@RequiredArgsConstructor
public class PublicController {

    private final ProductService  productService;
    private final CategoryService categoryService;
    private final ReviewService   reviewService;
    private final SellerService   sellerService;

    // Products

    @GetMapping("/api/products")
    public ResponseEntity<ApiResponse<PagedResponse<ProductResponse>>> getProducts(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String brand,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {

        PagedResponse<ProductResponse> result;
        if (q != null || categoryId != null || minPrice != null
                || maxPrice != null || brand != null) {
            result = productService.searchProducts(
                    q, categoryId, minPrice, maxPrice, brand, sortBy, sortDir, page, size
            );
        } else {
            result = productService.getPublicProducts(page, size, sortBy, sortDir);
        }
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/api/products/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProduct(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(productService.getProductByIdPublic(id)));
    }

    @GetMapping("/api/products/slug/{slug}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductBySlug(
            @PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.success(productService.getProductBySlug(slug)));
    }

    @GetMapping("/api/products/{id}/reviews")
    public ResponseEntity<ApiResponse<PagedResponse<ReviewResponse>>> getProductReviews(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                reviewService.getProductReviews(id, page, size)
        ));
    }

    // Sellers

    @GetMapping("/api/sellers/{id}")
    public ResponseEntity<ApiResponse<SellerPublicProfileResponse>> getSellerPublicProfile(
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(sellerService.getSellerPublicProfile(id)));
    }

    @GetMapping("/api/sellers/{id}/products")
    public ResponseEntity<ApiResponse<PagedResponse<ProductResponse>>> getSellerPublicProducts(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        return ResponseEntity.ok(ApiResponse.success(productService.getSellerProducts(id, page, size)));
    }

    // Customers (public reviewer profiles)

    @GetMapping("/api/customers/{id}")
    public ResponseEntity<ApiResponse<CustomerPublicProfileResponse>> getCustomerPublicProfile(
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(reviewService.getCustomerPublicProfile(id)));
    }

    @GetMapping("/api/customers/{id}/reviews")
    public ResponseEntity<ApiResponse<PagedResponse<ReviewResponse>>> getCustomerPublicReviews(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(reviewService.getCustomerReviews(id, page, size)));
    }

    // Categories

    @GetMapping("/api/categories")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getCategories() {
        return ResponseEntity.ok(ApiResponse.success(categoryService.getAllCategories()));
    }

    @GetMapping("/api/categories/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> getCategoryById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(categoryService.getCategoryById(id)));
    }

    @GetMapping("/api/categories/slug/{slug}")
    public ResponseEntity<ApiResponse<CategoryResponse>> getCategoryBySlug(
            @PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.success(categoryService.getCategoryBySlug(slug)));
    }

    @GetMapping("/api/categories/{id}/products")
    public ResponseEntity<ApiResponse<PagedResponse<ProductResponse>>> getProductsByCategory(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        return ResponseEntity.ok(ApiResponse.success(
                productService.getProductsByCategory(id, page, size, sortBy, sortDir)
        ));
    }
    // Search

    @GetMapping("/api/search")
    public ResponseEntity<ApiResponse<PagedResponse<ProductResponse>>> search(
            @RequestParam String q,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String brand,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                productService.searchProducts(q, categoryId, minPrice, maxPrice,
                        brand, sortBy, sortDir, page, size)
        ));
    }

    // Brands

    @GetMapping("/api/brands")
    public ResponseEntity<ApiResponse<List<String>>> getBrands() {
        return ResponseEntity.ok(ApiResponse.success(productService.getAllBrands()));
    }
}