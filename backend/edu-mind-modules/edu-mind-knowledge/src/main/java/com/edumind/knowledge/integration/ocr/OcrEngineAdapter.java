package com.edumind.knowledge.integration.ocr;

import java.util.List;

/**
 * OCR 引擎统一适配器接口
 */
public interface OcrEngineAdapter {

    /**
     * 引擎唯一标识编码 (如 PADDLE_OCR, MINERU, GPT4O_VISION)
     */
    String getEngineCode();

    /**
     * 是否支持指定引擎编码
     */
    boolean supports(String engine);

    /**
     * 执行 OCR 识别
     *
     * @param request 识别请求
     * @return 识别结果页面列表
     */
    List<OcrPageResult> recognize(OcrRecognizeRequest request);
}
