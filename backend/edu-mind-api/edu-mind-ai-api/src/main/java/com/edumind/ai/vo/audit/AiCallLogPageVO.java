package com.edumind.ai.vo.audit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * AI 调用明细日志分页数据对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiCallLogPageVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Builder.Default
    private List<AiCallLogVO> list = Collections.emptyList();

    @Builder.Default
    private Long total = 0L;

    @Builder.Default
    private Long pageNum = 1L;

    @Builder.Default
    private Long pageSize = 10L;
}
