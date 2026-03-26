package com.erandevu.util;

import com.erandevu.dto.response.PageResponse;
import org.springframework.data.domain.Page;

import java.util.List;

public class PageResponseUtil {

    public static <T> PageResponse<T> createPageResponse(Page<T> page) {
        return PageResponse.<T>builder()
                .content(page.getContent())
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .hasContent(page.hasContent())
                .numberOfElements(page.getNumberOfElements())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .build();
    }

    public static <T> PageResponse<T> createEmptyPageResponse(int pageNumber, int pageSize) {
        return PageResponse.<T>builder()
                .content(List.of())
                .pageNumber(pageNumber)
                .pageSize(pageSize)
                .totalElements(0)
                .totalPages(0)
                .first(true)
                .last(true)
                .hasContent(false)
                .numberOfElements(0)
                .hasNext(false)
                .hasPrevious(false)
                .build();
    }
}
