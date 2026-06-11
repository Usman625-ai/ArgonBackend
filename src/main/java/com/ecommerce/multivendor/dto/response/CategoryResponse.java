package com.ecommerce.multivendor.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data @Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CategoryResponse {
    private Long id;
    private String name;
    private String slug;
    private String description;
    private String imageUrl;
    private boolean active;
    private int displayOrder;
    private Long parentId;
    private String parentName;
    private List<CategoryResponse> children;
    private long productCount;
}
