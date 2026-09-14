package com.edumind.knowledge.integration.ocr.impl;

import com.edumind.knowledge.integration.ocr.OcrEngineAdapter;
import com.edumind.knowledge.integration.ocr.OcrPageResult;
import com.edumind.knowledge.integration.ocr.OcrRecognizeRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * PaddleOCR 真实引擎适配器 (Beta 阶段预留空壳)
 */
@Slf4j
@Component
public class PaddleOcrEngineAdapter implements OcrEngineAdapter {

    @Override
    public String getEngineCode() {
        return "PADDLE_OCR";
    }

    @Override
    public boolean supports(String engine) {
        return "PADDLE_OCR".equalsIgnoreCase(engine);
    }

    @Override
    public List<OcrPageResult> recognize(OcrRecognizeRequest request) {
        log.warn("[PaddleOcrEngine] PaddleOCR service is not deployed in current environment. Request taskId={}",
                request != null ? request.getTaskId() : null);
        throw new UnsupportedOperationException("PaddleOCR engine is not deployed in current environment. Please configure MockOcrEngineAdapter.");
    }
}
