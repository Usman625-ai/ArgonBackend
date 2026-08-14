package com.ecommerce.multivendor.service.impl;

import com.ecommerce.multivendor.config.RetryableRead;
import com.ecommerce.multivendor.dto.request.ProductRequest;
import com.ecommerce.multivendor.dto.request.StockUpdateRequest;
import com.ecommerce.multivendor.dto.response.*;
import com.ecommerce.multivendor.entity.*;
import com.ecommerce.multivendor.exception.BadRequestException;
import com.ecommerce.multivendor.exception.ResourceNotFoundException;
import com.ecommerce.multivendor.exception.UnauthorizedException;
import com.ecommerce.multivendor.repository.*;
import com.ecommerce.multivendor.util.SlugUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final CategoryRepository categoryRepository;
    private final CloudinaryService cloudinaryService;
    private final SiteSettingRepository siteSettingRepository;
    private final NotificationService notificationService;
    private final OrderItemRepository orderItemRepository;

    // ─── Public: Browse / Search ───────────────────────────────────────────

    @RetryableRead
    @Transactional(readOnly = true)
    public PagedResponse<ProductResponse> getPublicProducts(int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, Math.min(size, 100), sort);
        Page<Product> productPage = productRepository.findByActiveTrue(pageable);
        return toPagedResponse(productPage);
    }

    @RetryableRead
    @Transactional(readOnly = true)
    public PagedResponse<ProductResponse> getAdminProducts(String q, Boolean active, int page, int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100), Sort.by("createdAt").descending());
        String query = (q != null && !q.isBlank()) ? q.trim() : null;
        Page<Product> productPage = productRepository.findAllForAdmin(query, active, pageable);
        return toPagedResponse(productPage);
    }

    @RetryableRead
    @Transactional(readOnly = true)
    public PagedResponse<ProductResponse> searchProducts(String query, Long categoryId,
                                                         BigDecimal minPrice, BigDecimal maxPrice,
                                                         String brand, String sortBy, String sortDir,
                                                         int page, int size) {
        Sort sort = buildSort(sortBy, sortDir);
        Pageable pageable = PageRequest.of(page, Math.min(size, 100), sort);
        Page<Product> productPage = productRepository.filterProducts(
                categoryId, minPrice, maxPrice, brand, query, pageable
        );
        return toPagedResponse(productPage);
    }

    @RetryableRead
    @Transactional(readOnly = true)
    public ProductResponse getProductByIdPublic(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
        if (!product.isActive()) {
            throw new ResourceNotFoundException("Product", id);
        }
        return toProductResponse(product);
    }

    @RetryableRead
    @Transactional(readOnly = true)
    public ProductResponse getProductBySlug(String slug) {
        Product product = productRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + slug));
        if (!product.isActive()) {
            throw new ResourceNotFoundException("Product not found: " + slug);
        }
        return toProductResponse(product);
    }

    @Transactional(readOnly = true)
    @Cacheable("brands")
    public List<String> getAllBrands() {
        return productRepository.findAllBrands();
    }

    @RetryableRead
    @Transactional(readOnly = true)
    public PagedResponse<ProductResponse> getProductsByCategory(Long categoryId,
                                                                int page, int size,
                                                                String sortBy, String sortDir) {
        Sort sort = buildSort(sortBy, sortDir);
        Pageable pageable = PageRequest.of(page, Math.min(size, 100), sort);

        // Optional: include products from subcategories as well
        // For now, only direct category
        Page<Product> productPage = productRepository.findByCategoryIdAndActiveTrue(categoryId, pageable);

        // If you need products from all subcategories, use a recursive query or fetch children first
        // See note below

        return toPagedResponse(productPage);
    }

    // ─── Seller: Manage Products ───────────────────────────────────────────

    @RetryableRead
    @Transactional(readOnly = true)
    public PagedResponse<ProductResponse> getSellerProducts(Long sellerId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Product> productPage = productRepository.findBySellerIdAndActiveTrue(sellerId, pageable);
        return toPagedResponse(productPage);
    }

    @RetryableRead
    @Transactional(readOnly = true)
    public PagedResponse<ProductResponse> getAllSellerProducts(Long sellerId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Product> productPage = productRepository.findBySellerId(sellerId, pageable);
        return toPagedResponse(productPage);
    }

    @Transactional(readOnly = true)
    public ProductResponse getSellerProduct(Long productId, Long sellerId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));
        verifySellerOwnership(product, sellerId);
        return toProductResponse(product);
    }

    public ProductResponse createProduct(ProductRequest request, User seller) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", request.getCategoryId()));

        if (!category.isActive()) {
            throw new BadRequestException("Cannot add product to an inactive category");
        }

        String slug = generateUniqueSlug(request.getName());

        String primaryImageUrl = request.getPrimaryImageUrl();
        if ((primaryImageUrl == null || primaryImageUrl.isBlank())
                && request.getImageUrls() != null && !request.getImageUrls().isEmpty()) {
            primaryImageUrl = request.getImageUrls().get(0);
        }
        if (primaryImageUrl == null || primaryImageUrl.isBlank()) {
            throw new BadRequestException("At least one product image is required");
        }

        // Must run BEFORE the builder below reads request.getSpecifications() — an
        // empty string is not valid JSON and TiDB rejects it on insert for this
        // JSON-typed column ("Data truncation: Invalid JSON text").
        if (request.getSpecifications() != null && request.getSpecifications().isBlank()) {
            request.setSpecifications(null);
        }

        Product product = Product.builder()
                .name(request.getName())
                .slug(slug)
                .description(request.getDescription())
                .shortDescription(request.getShortDescription())
                .price(request.getPrice())
                .discountedPrice(request.getDiscountedPrice())
                .stockQuantity(request.getStockQuantity())
                .brand(request.getBrand())
                .tags(request.getTags())
                .specifications(request.getSpecifications())
                .category(category)
                .seller(seller)
                .featured(request.isFeatured())
                .primaryImageUrl(primaryImageUrl)
                .active(true)
                .build();

        product = productRepository.save(product);

        // Save product images
        if (request.getImageUrls() != null && !request.getImageUrls().isEmpty()) {
            saveProductImages(product, request.getImageUrls());
        }
        log.info("Product created: {} by seller: {}", product.getId(), seller.getId());

        // Notify admin for moderation if needed (optional)
        return toProductResponse(product);
    }

    public ProductResponse updateProduct(Long productId, ProductRequest request, Long sellerId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));
        verifySellerOwnership(product, sellerId);

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", request.getCategoryId()));

        // Must run BEFORE product.setSpecifications() below — see same note in createProduct().
        if (request.getSpecifications() != null && request.getSpecifications().isBlank()) {
            request.setSpecifications(null);
        }

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setShortDescription(request.getShortDescription());
        product.setPrice(request.getPrice());
        product.setDiscountedPrice(request.getDiscountedPrice());
        product.setStockQuantity(request.getStockQuantity());
        product.setBrand(request.getBrand());
        product.setTags(request.getTags());
        product.setSpecifications(request.getSpecifications());
        product.setCategory(category);
        product.setFeatured(request.isFeatured());
        // Only overwrite the primary image if a real value was supplied.
        // Falling back to null here violates the NOT NULL DB constraint whenever
        // the seller edits a product without re-selecting a primary image.
        if (request.getPrimaryImageUrl() != null && !request.getPrimaryImageUrl().isBlank()) {
            product.setPrimaryImageUrl(request.getPrimaryImageUrl());
        } else if (request.getImageUrls() != null && !request.getImageUrls().isEmpty()) {
            product.setPrimaryImageUrl(request.getImageUrls().get(0));
        }
        // else: keep the existing primaryImageUrl untouched

        // Update images if provided
        if (request.getImageUrls() != null && !request.getImageUrls().isEmpty()) {
            // Remove old images and replace
            List<ProductImage> oldImages = productImageRepository.findByProductId(productId);
            oldImages.forEach(img -> {
                if (img.getPublicId() != null) {
                    cloudinaryService.deleteImage(img.getPublicId());
                }
            });
            productImageRepository.deleteByProductId(productId);
            saveProductImages(product, request.getImageUrls());
        }

        product = productRepository.save(product);
        log.info("Product updated: {}", productId);
        return toProductResponse(product);
    }

    public ProductResponse toggleSellerProductStatus(Long productId, Long sellerId, boolean active) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));
        verifySellerOwnership(product, sellerId);

        if (active && product.isAdminLocked()) {
            throw new BadRequestException(
                    "This product was deactivated by an admin and can only be reactivated by an admin.");
        }

        product.setActive(active);
        // A seller-initiated deactivation is never admin-locked, so it stays
        // freely re-activatable by the seller themselves.
        if (!active) product.setAdminLocked(false);
        Product updated = productRepository.save(product);
        log.info("Product {} {} by seller {}", productId, active ? "activated" : "deactivated", sellerId);
        return toProductResponse(updated);
    }

    public void deleteProduct(Long productId, Long sellerId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));
        verifySellerOwnership(product, sellerId);

        List<ProductImage> images = productImageRepository.findByProductId(productId);
        List<String> publicIds = images.stream()
                .map(ProductImage::getPublicId)
                .filter(id -> id != null && !id.isBlank())
                .toList();

        boolean hasOrderHistory = orderItemRepository.existsByProductId(productId);

        if (!hasOrderHistory) {
            // Safe to permanently delete: no past orders reference this product.
            if (!publicIds.isEmpty()) cloudinaryService.deleteImages(publicIds);
            productRepository.delete(product); // cascades to images/reviews/cart items/wishlist entries
            log.info("Product {} permanently deleted (with {} Cloudinary image(s)) by seller {}",
                    productId, publicIds.size(), sellerId);
            return;
        }

        // Product has order history — a hard delete would violate the required
        // OrderItem -> Product foreign key and corrupt past order records.
        // Archive it instead (deactivate + strip its images) so it disappears
        // from the storefront and the seller's active product list, while
        // preserving order history integrity.
        if (!publicIds.isEmpty()) cloudinaryService.deleteImages(publicIds);
        productImageRepository.deleteByProductId(productId);
        product.setActive(false);
        productRepository.save(product);
        log.info("Product {} has order history — archived (deactivated, images removed) instead of hard-deleted", productId);
    }


    public ProductResponse updateStock(Long productId, StockUpdateRequest request, Long sellerId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));
        verifySellerOwnership(product, sellerId);

        product.setStockQuantity(request.getQuantity());
        product = productRepository.save(product);
        log.info("Stock updated for product {} to {}", productId, request.getQuantity());

        // Low stock notification
        if (request.getQuantity() <= 10) {
            notificationService.createNotification(
                    product.getSeller(),
                    "Stock alert: " + product.getName() + " has only " + request.getQuantity() + " units left.",
                    "Low Stock Alert",
                    com.ecommerce.multivendor.enums.NotificationType.LOW_STOCK,
                    "/seller/products/" + productId
            );
        }

        return toProductResponse(product);
    }

    // ─── Image Upload (multipart) ──────────────────────────────────────────

    public List<String> uploadProductImages(Long productId, List<MultipartFile> files, Long sellerId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));
        verifySellerOwnership(product, sellerId);

        List<String> uploadedUrls = new ArrayList<>();
        int order = productImageRepository.findByProductId(productId).size();

        for (MultipartFile file : files) {
            var result = cloudinaryService.uploadImage(file, "products");
            ProductImage image = ProductImage.builder()
                    .product(product)
                    .imageUrl(result.get("url"))
                    .publicId(result.get("publicId"))
                    .primary(order == 0)
                    .displayOrder(order++)
                    .build();
            productImageRepository.save(image);
            uploadedUrls.add(result.get("url"));
        }

        return uploadedUrls;
    }

    public void deleteProductImage(Long productId, Long imageId, Long sellerId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));
        verifySellerOwnership(product, sellerId);

        ProductImage image = productImageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Image", imageId));

        if (image.getPublicId() != null) {
            cloudinaryService.deleteImage(image.getPublicId());
        }
        productImageRepository.delete(image);
    }

    // ─── Rating update (called internally after review) ───────────────────

    public void recalculateProductRating(Long productId) {
        // This is called from ReviewService after each review add/update/delete
        productRepository.findById(productId).ifPresent(product -> {
            long reviewCount = product.getReviews().size();
            if (reviewCount == 0) {
                productRepository.updateRatingStats(productId, BigDecimal.ZERO, 0);
            } else {
                double avg = product.getReviews().stream()
                        .mapToInt(r -> r.getRating())
                        .average()
                        .orElse(0.0);
                productRepository.updateRatingStats(
                        productId,
                        BigDecimal.valueOf(avg).setScale(2, java.math.RoundingMode.HALF_UP),
                        (int) reviewCount
                );
            }
        });
    }

    // ─── Admin ─────────────────────────────────────────────────────────────

    @RetryableRead
    @Transactional(readOnly = true)
    public PagedResponse<ProductResponse> getAllProductsAdmin(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Product> productPage = productRepository.findAll(pageable);
        return toPagedResponse(productPage);
    }

    @Transactional(readOnly = true)
    public List<Product> getLowStockProducts() {
        return productRepository.findLowStockProducts(10);
    }

    // ─── Mappers ───────────────────────────────────────────────────────────

    public ProductResponse toProductResponse(Product product) {
        List<ProductImage> images = productImageRepository.findByProductId(product.getId());
        return buildProductResponse(product, images);
    }

    private ProductResponse buildProductResponse(Product product, List<ProductImage> images) {
        ProductImage primary = images.stream().filter(ProductImage::isPrimary).findFirst()
                .orElse(images.isEmpty() ? null : images.get(0));

        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .slug(product.getSlug())
                .description(product.getDescription())
                .shortDescription(product.getShortDescription())
                .price(product.getPrice())
                .discountedPrice(product.getDiscountedPrice())
                .effectivePrice(product.getEffectivePrice())
                .stockQuantity(product.getStockQuantity())
                .inStock(product.isInStock())
                .brand(product.getBrand())
                .tags(product.getTags())
                .specifications(product.getSpecifications())
                .averageRating(product.getAverageRating())
                .totalReviews(product.getTotalReviews())
                .totalSold(product.getTotalSold())
                .active(product.isActive())
                .adminLocked(product.isAdminLocked())
                .featured(product.isFeatured())
                .categoryId(product.getCategory().getId())
                .categoryName(product.getCategory().getName())
                .sellerId(product.getSeller().getId())
                .sellerName(product.getSeller().getName())
                .shopName(product.getSeller().getShopName())
                .sellerProfileImage(product.getSeller().getShopLogo())
                .images(images.stream().map(img ->
                        ProductImageResponse.builder()
                                .id(img.getId())
                                .imageUrl(img.getImageUrl())
                                .primary(img.isPrimary())
                                .displayOrder(img.getDisplayOrder())
                                .build()
                ).toList())
                .primaryImageUrl(primary != null ? primary.getImageUrl() : null)
                .createdAt(product.getCreatedAt())
                .build();
    }

    /**
     * Maps a page of products in bulk: ONE query to fetch every image for every
     * product on the page (instead of one query per product — a classic N+1 that's
     * especially costly when the database isn't on localhost, since every extra
     * round trip pays full network latency). Category/seller N+1 is avoided
     * separately via @EntityGraph on the repository query methods that feed this.
     */
    private PagedResponse<ProductResponse> toPagedResponse(Page<Product> page) {
        List<Product> products = page.getContent();
        List<Long> productIds = products.stream().map(Product::getId).toList();

        Map<Long, List<ProductImage>> imagesByProduct = productIds.isEmpty()
                ? Map.of()
                : productImageRepository.findByProductIdIn(productIds).stream()
                .collect(Collectors.groupingBy(img -> img.getProduct().getId()));

        List<ProductResponse> content = products.stream()
                .map(p -> buildProductResponse(p, imagesByProduct.getOrDefault(p.getId(), List.of())))
                .toList();

        return PagedResponse.<ProductResponse>builder()
                .content(content)
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .first(page.isFirst())
                .build();
    }

    // ─── Private helpers ───────────────────────────────────────────────────

    private void verifySellerOwnership(Product product, Long sellerId) {
        if (!product.getSeller().getId().equals(sellerId)) {
            throw new UnauthorizedException("You are not authorized to manage this product");
        }
    }

    private String generateUniqueSlug(String name) {
        String base = SlugUtils.toSlug(name);
        String slug = base;
        int counter = 1;
        while (productRepository.existsBySlug(slug)) {
            slug = base + "-" + counter++;
        }
        return slug;
    }

    private void saveProductImages(Product product, List<String> imageUrls) {
        for (int i = 0; i < imageUrls.size(); i++) {
            ProductImage img = ProductImage.builder()
                    .product(product)
                    .imageUrl(imageUrls.get(i))
                    .primary(i == 0)
                    .displayOrder(i)
                    .build();
            productImageRepository.save(img);
        }
    }

    private Sort buildSort(String sortBy, String sortDir) {
        String column = switch (sortBy.toLowerCase()) {
            case "price" -> "price";
            case "rating" -> "averageRating";
            case "popular" -> "totalSold";
            default -> "createdAt";
        };
        return sortDir.equalsIgnoreCase("asc")
                ? Sort.by(column).ascending()
                : Sort.by(column).descending();
    }

    //    Featured product
    @Transactional(readOnly = true)
    public ApiResponse<List<FeaturedProductResponse>> getFeaturedProducts(Integer limit) {
        List<Product> products;

        if (limit != null && limit > 0) {
            Pageable pageable = PageRequest.of(0, limit);
            products = productRepository.findFeaturedProducts(pageable).getContent();
        } else {
            products = productRepository.findFeaturedProducts();
        }

        String currencySymbol = siteSettingRepository.findTopByOrderByIdAsc()
                .map(SiteSetting::getCurrencySymbol)
                .orElse("PKR");

        List<FeaturedProductResponse> response = products.stream()
                .map(p -> mapToFeaturedResponse(p, currencySymbol))
                .toList();

        return ApiResponse.success("Featured products retrieved successfully", response);
    }

    private FeaturedProductResponse mapToFeaturedResponse(Product p, String currencySymbol) {
        return FeaturedProductResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .slug(p.getSlug())
                .description(p.getDescription())
                .price(p.getPrice())
                .discountedPrice(p.getDiscountedPrice())
                .currencySymbol(currencySymbol)
                .stockQuantity(p.getStockQuantity())
                .primaryImageUrl(p.getPrimaryImageUrl())
                .images(p.getImages().stream()
                        .map(img -> ProductImageResponse.builder()
                                .id(img.getId())
                                .imageUrl(img.getImageUrl())
                                .primary(img.isPrimary())
                                .displayOrder(img.getDisplayOrder())
                                .build())
                        .toList())
                .averageRating(p.getAverageRating() != null ? p.getAverageRating().doubleValue() : 0.0)
                .reviewCount(p.getTotalReviews())
                .categoryName(p.getCategory() != null ? p.getCategory().getName() : null)
                .sellerShopName(p.getSeller() != null ? p.getSeller().getShopName() : null)
                .featuredAt(p.getFeaturedAt())
                .build();
    }
}