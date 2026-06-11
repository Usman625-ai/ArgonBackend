package com.ecommerce.multivendor.service.impl;

import com.ecommerce.multivendor.dto.request.CategoryRequest;
import com.ecommerce.multivendor.dto.response.CategoryResponse;
import com.ecommerce.multivendor.entity.Category;
import com.ecommerce.multivendor.exception.BadRequestException;
import com.ecommerce.multivendor.exception.ResourceNotFoundException;
import com.ecommerce.multivendor.repository.CategoryRepository;
import com.ecommerce.multivendor.repository.ProductRepository;
import com.ecommerce.multivendor.util.SlugUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    // ─── Public: all active categories with children ───────────────────────

    @Transactional(readOnly = true)
    @Cacheable("categories")
    public List<CategoryResponse> getAllCategories() {
        List<Category> roots = categoryRepository.findRootCategoriesOrdered();
        return roots.stream().map(this::toCategoryResponseWithChildren).toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponse getCategoryBySlug(String slug) {
        Category category = categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + slug));
        return toCategoryResponseWithChildren(category);
    }

    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));
        return toCategoryResponseWithChildren(category);
    }

//    @Transactional(readOnly = true)
//    public List<CategoryResponse> getSubcategories(Long parentId) {
//        List<Category> subcats = categoryRepository.findActiveSubcategoriesByParentId(parentId);
//        return subcats.stream()
//                .map(this::toCategoryResponse)   // or toCategoryResponseWithChildren if you want nested
//                .toList();
//    }

    // ─── Admin: CRUD ───────────────────────────────────────────────────────

    @CacheEvict(value = "categories", allEntries = true)
    public CategoryResponse createCategory(CategoryRequest request) {
        if (categoryRepository.existsByName(request.getName())) {
            throw new BadRequestException("Category with name '" + request.getName() + "' already exists");
        }

        String slug = generateUniqueSlug(request.getName());

        Category category = Category.builder()
                .name(request.getName())
                .slug(slug)
                .description(request.getDescription())
                .imageUrl(request.getImageUrl())
                .active(request.isActive())
                .displayOrder(request.getDisplayOrder())
                .build();

        if (request.getParentId() != null) {
            Category parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent category", request.getParentId()));
            category.setParent(parent);
        }

        category = categoryRepository.save(category);
        log.info("Category created: {} [{}]", category.getName(), category.getId());
        return toCategoryResponse(category);
    }

    @CacheEvict(value = "categories", allEntries = true)
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));

        // Check name uniqueness only if changed
        if (!category.getName().equals(request.getName()) &&
                categoryRepository.existsByName(request.getName())) {
            throw new BadRequestException("Category name already taken");
        }

        category.setName(request.getName());
        category.setDescription(request.getDescription());
        category.setImageUrl(request.getImageUrl());
        category.setActive(request.isActive());
        category.setDisplayOrder(request.getDisplayOrder());

        if (request.getParentId() != null) {
            if (request.getParentId().equals(id)) {
                throw new BadRequestException("Category cannot be its own parent");
            }
            Category parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent category", request.getParentId()));
            category.setParent(parent);
        } else {
            category.setParent(null);
        }

        category = categoryRepository.save(category);
        log.info("Category updated: {}", id);
        return toCategoryResponse(category);
    }

    @CacheEvict(value = "categories", allEntries = true)
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));

        if (!category.getChildren().isEmpty()) {
            throw new BadRequestException("Cannot delete category with sub-categories. Delete children first.");
        }
        if (!category.getProducts().isEmpty()) {
            throw new BadRequestException("Cannot delete category with associated products.");
        }

        categoryRepository.delete(category);
        log.info("Category deleted: {}", id);
    }

    // ─── Mappers ───────────────────────────────────────────────────────────

    public CategoryResponse toCategoryResponseWithChildren(Category category) {
        CategoryResponse response = toCategoryResponse(category);
        if (category.getChildren() != null && !category.getChildren().isEmpty()) {
            response.setChildren(
                    category.getChildren().stream()
                            .filter(Category::isActive)
                            .map(this::toCategoryResponseWithChildren)
                            .toList()
            );
        }
        return response;
    }

    public CategoryResponse toCategoryResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .description(category.getDescription())
                .imageUrl(category.getImageUrl())
                .active(category.isActive())
                .displayOrder(category.getDisplayOrder())
                .parentId(category.getParent() != null ? category.getParent().getId() : null)
                .parentName(category.getParent() != null ? category.getParent().getName() : null)
                .build();
    }

    // ─── Slug helper ───────────────────────────────────────────────────────

    private String generateUniqueSlug(String name) {
        String baseSlug = SlugUtils.toSlug(name);
        String slug = baseSlug;
        int counter = 1;
        while (categoryRepository.existsBySlug(slug)) {
            slug = baseSlug + "-" + counter++;
        }
        return slug;
    }
}