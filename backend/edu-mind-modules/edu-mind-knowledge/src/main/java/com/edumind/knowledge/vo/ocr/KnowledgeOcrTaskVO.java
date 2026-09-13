package com.edumind.knowledge.vo.ocr;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class KnowledgeOcrTaskVO {
    private Long id;
    private Long tenantId;
    private Long documentId;
    private String engine;
    private Integer totalPages;
    private Integer processedPages;
    private String status;
    private Integer progress;
    private String errorMsg;
    private LocalDateTime createTime;
}
