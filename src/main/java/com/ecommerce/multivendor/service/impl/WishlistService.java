package com.ecommerce.multivendor.service.impl;

import com.ecommerce.multivendor.dto.response.PagedResponse;
import com.ecommerce.multivendor.dto.response.ProductResponse;
import com.ecommerce.multivendor.entity.User;
import com.ecommerce.multivendor.entity.Wishlist;
import com.ecommerce.multivendor.exception.BadRequestException;
import com.ecommerce.multivendor.exception.ResourceNotFoundException;
import com.ecommerce.multivendor.repository.ProductRepository;
import com.ecommerce.multivendor.repository.WishlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final ProductRepository productRepository;
    private final ProductService productService;

    public void addToWishlist(Long productId, User user) {
        productRepository.findById(productId)
            .orElseThrow(() -> new ResourceNotFoundException("Product", productId));

        if (wishlistRepository.existsByUserIdAndProductId(user.getId(), productId)) {
            throw new BadRequestException("Product already in wishlist");
        }

        Wishlist wishlist = Wishlist.builder()
            .user(user)
            .product(productRepository.getReferenceById(productId))
            .build();
        wishlistRepository.save(wishlist);
    }

    public void removeFromWishlist(Long productId, Long userId) {
        if (!wishlistRepository.existsByUserIdAndProductId(userId, productId)) {
            throw new ResourceNotFoundException("Product not in wishlist");
        }
        wishlistRepository.deleteByUserIdAndProductId(userId, productId);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ProductResponse> getWishlist(Long userId, int page, int size) {
        Page<Wishlist> wishlistPage = wishlistRepository.findByUserId(
            userId, PageRequest.of(page, size)
        );
        return PagedResponse.<ProductResponse>builder()
            .content(wishlistPage.getContent().stream()
                .map(w -> productService.toProductResponse(w.getProduct())).toList())
            .pageNumber(wishlistPage.getNumber())
            .pageSize(wishlistPage.getSize())
            .totalElements(wishlistPage.getTotalElements())
            .totalPages(wishlistPage.getTotalPages())
            .last(wishlistPage.isLast())
            .first(wishlistPage.isFirst())
            .build();
    }
}
