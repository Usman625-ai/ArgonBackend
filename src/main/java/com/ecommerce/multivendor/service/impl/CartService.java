package com.ecommerce.multivendor.service.impl;

import com.ecommerce.multivendor.dto.request.CartRequest;
import com.ecommerce.multivendor.dto.response.CartItemResponse;
import com.ecommerce.multivendor.dto.response.CartResponse;
import com.ecommerce.multivendor.entity.CartItem;
import com.ecommerce.multivendor.entity.Product;
import com.ecommerce.multivendor.entity.ProductImage;
import com.ecommerce.multivendor.entity.User;
import com.ecommerce.multivendor.exception.BadRequestException;
import com.ecommerce.multivendor.exception.ResourceNotFoundException;
import com.ecommerce.multivendor.repository.CartItemRepository;
import com.ecommerce.multivendor.repository.ProductImageRepository;
import com.ecommerce.multivendor.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;

    // ─── Add to cart ───────────────────────────────────────────────────────

    public CartResponse addToCart(User user, CartRequest request) {
        Product product = productRepository.findById(request.getProductId())
            .orElseThrow(() -> new ResourceNotFoundException("Product", request.getProductId()));

        if (!product.isActive() || !product.getSeller().isActive()) {
            throw new BadRequestException("This product is not available");
        }
        if (!product.isInStock()) {
            throw new BadRequestException("Product is out of stock");
        }
        if (request.getQuantity() > product.getStockQuantity()) {
            throw new BadRequestException("Requested quantity exceeds available stock ("
                + product.getStockQuantity() + " available)");
        }

        Optional<CartItem> existingItem = cartItemRepository
            .findByUserIdAndProductId(user.getId(), product.getId());

        if (existingItem.isPresent()) {
            // Update quantity
            CartItem item = existingItem.get();
            int newQty = item.getQuantity() + request.getQuantity();
            if (newQty > product.getStockQuantity()) {
                throw new BadRequestException("Total quantity exceeds available stock");
            }
            item.setQuantity(newQty);
            cartItemRepository.save(item);
        } else {
            // Add new item
            CartItem cartItem = CartItem.builder()
                .user(user)
                .product(product)
                .quantity(request.getQuantity())
                .build();
            cartItemRepository.save(cartItem);
        }

        return getCart(user.getId());
    }

    // ─── Get cart ──────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public CartResponse getCart(Long userId) {
        List<CartItem> items = cartItemRepository.findByUserId(userId);

        List<CartItemResponse> itemResponses = items.stream()
            .map(this::toCartItemResponse)
            .toList();

        BigDecimal subtotal = itemResponses.stream()
            .map(CartItemResponse::getItemTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return CartResponse.builder()
            .items(itemResponses)
            .totalItems(itemResponses.stream().mapToInt(CartItemResponse::getQuantity).sum())
            .subtotal(subtotal)
            .discount(BigDecimal.ZERO)
            .total(subtotal)
            .build();
    }

    // ─── Update quantity ───────────────────────────────────────────────────

    public CartResponse updateCartItem(Long cartItemId, int quantity, Long userId) {
        CartItem item = cartItemRepository.findById(cartItemId)
            .orElseThrow(() -> new ResourceNotFoundException("Cart item", cartItemId));

        if (!item.getUser().getId().equals(userId)) {
            throw new BadRequestException("Cart item does not belong to you");
        }
        if (quantity <= 0) {
            cartItemRepository.delete(item);
        } else {
            if (quantity > item.getProduct().getStockQuantity()) {
                throw new BadRequestException("Quantity exceeds available stock");
            }
            item.setQuantity(quantity);
            cartItemRepository.save(item);
        }

        return getCart(userId);
    }

    // ─── Remove item ───────────────────────────────────────────────────────

    public CartResponse removeFromCart(Long cartItemId, Long userId) {
        CartItem item = cartItemRepository.findById(cartItemId)
            .orElseThrow(() -> new ResourceNotFoundException("Cart item", cartItemId));

        if (!item.getUser().getId().equals(userId)) {
            throw new BadRequestException("Cart item does not belong to you");
        }

        cartItemRepository.delete(item);
        return getCart(userId);
    }

    // ─── Clear cart ────────────────────────────────────────────────────────

    public void clearCart(Long userId) {
        cartItemRepository.deleteByUserId(userId);
    }

    // ─── Get raw items for checkout ────────────────────────────────────────

    public List<CartItem> getCartItems(Long userId) {
        return cartItemRepository.findByUserId(userId);
    }

    // ─── Mapper ────────────────────────────────────────────────────────────

    private CartItemResponse toCartItemResponse(CartItem item) {
        Product product = item.getProduct();

        // Get primary image
        String imageUrl = productImageRepository.findByProductIdAndPrimaryTrue(product.getId())
            .map(ProductImage::getImageUrl)
            .orElse(null);

        BigDecimal effectivePrice = product.getEffectivePrice();
        BigDecimal itemTotal = effectivePrice.multiply(BigDecimal.valueOf(item.getQuantity()));

        return CartItemResponse.builder()
            .id(item.getId())
            .productId(product.getId())
            .productName(product.getName())
            .productImage(imageUrl)
            .unitPrice(product.getPrice())
            .effectivePrice(effectivePrice)
            .quantity(item.getQuantity())
            .itemTotal(itemTotal)
            .availableStock(product.getStockQuantity())
            .inStock(product.isInStock())
            .build();
    }
}
