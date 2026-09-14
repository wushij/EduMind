package com.edumind.knowledge.dto.ocr;

import lombok.Data;

import java.io.Serializable;

/**
 * 更新 OCR 识别页校对文本请求 DTO
 */
@Data
public class OcrPageUpdateDTO implements Serializable {

    private String proofreadText;
}
