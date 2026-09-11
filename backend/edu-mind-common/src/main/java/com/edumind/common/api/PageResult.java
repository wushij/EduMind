package com.edumind.common.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> implements Serializable {
    private Long total;
    private Long pageNum;
    private Long page;
    private Long pageSize;
    private List<T> list;

    public Long getPage() {
        return page != null ? page : pageNum;
    }

    public Long getPageNum() {
        return pageNum != null ? pageNum : page;
    }

    public static <T> PageResult<T> empty(Long pageNum, Long pageSize) {
        return PageResult.<T>builder()
                .total(0L)
                .pageNum(pageNum)
                .pageSize(pageSize)
                .list(Collections.emptyList())
                .build();
    }
}