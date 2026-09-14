package com.edumind.knowledge.integration.ocr;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * OCR 单页识别结果模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OcrPageResult implements Serializable {
    private Integer pageNo;
    private String rawText;
    private String blocksJson;
    private BigDecimal confidenceScore;
}
