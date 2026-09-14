package com.edumind.knowledge.dto.ocr;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 创建 OCR 任务请求 DTO
 */
@Data
public class OcrTaskCreateDTO implements Serializable {

    @NotNull(message = "关联文档ID不能为空")
    private Long documentId;

    /**
     * 引擎类型 (PADDLE_OCR / MINERU / GPT4O_VISION)
     */
    private String engine;
}
