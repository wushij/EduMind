package com.edumind.knowledge.integration.ocr;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * OCR 识别请求参数对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OcrRecognizeRequest implements Serializable {
    private Long taskId;
    private Long documentId;
    private String engine;
    private Long tenantId;
    private boolean forceFail;
}
