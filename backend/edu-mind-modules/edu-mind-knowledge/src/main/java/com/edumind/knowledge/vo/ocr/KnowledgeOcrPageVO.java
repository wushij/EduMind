package com.edumind.knowledge.vo.ocr;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class KnowledgeOcrPageVO {
    private Long id;
    private Long taskId;
    private Integer pageNo;
    private String rawText;
    private String proofreadText;
    private String blocksJson;
    private BigDecimal confidenceScore;
    private Boolean proofreadStatus;
}
