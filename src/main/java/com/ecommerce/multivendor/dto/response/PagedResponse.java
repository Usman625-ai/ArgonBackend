package com.ecommerce.multivendor.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data @Builder
public class PagedResponse<T> {
    private List<T> content;
    private int pageNumber;
    private int pageSize;
    private long totalElements;
    private int totalPages;
    private boolean last;
    private boolean first;
}
