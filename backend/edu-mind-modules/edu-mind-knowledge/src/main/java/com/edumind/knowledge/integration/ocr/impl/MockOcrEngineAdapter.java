package com.edumind.knowledge.integration.ocr.impl;

import com.edumind.knowledge.integration.ocr.OcrEngineAdapter;
import com.edumind.knowledge.integration.ocr.OcrPageResult;
import com.edumind.knowledge.integration.ocr.OcrRecognizeRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Mock OCR 引擎适配器 (Beta 环境默认实现)
 */
@Slf4j
@Primary
@Component
@ConditionalOnProperty(prefix = "knowledge.ocr", name = "mock-enabled", havingValue = "true", matchIfMissing = true)
public class MockOcrEngineAdapter implements OcrEngineAdapter {

    private static volatile boolean forceFail = false;
    private static volatile int mockDelayMs = 150;

    public static void setForceFail(boolean fail) {
        forceFail = fail;
    }

    public static boolean isForceFail() {
        return forceFail;
    }

    public static void setMockDelayMs(int delayMs) {
        mockDelayMs = delayMs;
    }

    @Override
    public String getEngineCode() {
        return "MOCK";
    }

    @Override
    public boolean supports(String engine) {
        // Beta 阶段默认兜底支持所有请求引擎
        return true;
    }

    @Override
    public List<OcrPageResult> recognize(OcrRecognizeRequest request) {
        log.info("[MockOcrEngine] Starting OCR recognition for taskId={}, documentId={}, engine={}",
                request.getTaskId(), request.getDocumentId(), request.getEngine());

        if (forceFail || (request != null && request.isForceFail())) {
            log.warn("[MockOcrEngine] Simulated failure triggered for taskId={}", request != null ? request.getTaskId() : null);
            throw new RuntimeException("Simulated OCR engine failure: recognition pipeline crashed");
        }

        List<OcrPageResult> results = new ArrayList<>();
        for (int p = 1; p <= 3; p++) {
            if (mockDelayMs > 0) {
                try {
                    Thread.sleep(mockDelayMs);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }

            OcrPageResult page = OcrPageResult.builder()
                    .pageNo(p)
                    .rawText(getMockPageText(p))
                    .blocksJson(getMockBlocksJson(p))
                    .confidenceScore(BigDecimal.valueOf(96.50 + (p * 0.5)))
                    .build();
            results.add(page);
        }

        log.info("[MockOcrEngine] Finished recognition for taskId={}, totalPages={}",
                request.getTaskId(), results.size());
        return results;
    }

    private String getMockPageText(int pageNo) {
        if (pageNo == 1) {
            return "## 第一章：极限论基础与无穷小分析\n\n定义 1.1（极限的存在准则）设函数 $f(x)$ 在点 $x_0$ 的去心邻域内有定义。如果对于任意给定的正数 $\\varepsilon > 0$，总存在正数 $\\delta > 0$，使得对于所有满足 $0 < |x - x_0| < \\delta$ 的 $x$，恒有 $|f(x) - A| < \\varepsilon$，则称常数 $A$ 为函数 $f(x)$ 当 $x \\to x_0$ 时的极限，记作 $\\lim_{x \\to x_0} f(x) = A$。";
        } else if (pageNo == 2) {
            return "定理 1.2（等价无穷小替换定理）设 $\\alpha \\sim \\alpha', \\beta \\sim \\beta'$，且 $\\lim \\frac{\\beta'}{\\alpha'}$ 存在，则 $\\lim \\frac{\\beta}{\\alpha} = \\lim \\frac{\\beta'}{\\alpha'}$。\n\n【注意】等价无穷小替换原则上只适用于乘积与商的形式，在代数和中不能随意部分代换，必须满足泰勒高阶展开相同条件。";
        } else {
            return "例题 1.3 求极限 $\\lim_{x \\to 0} \\frac{\\tan x - \\sin x}{x^3}$。\n\n【解析】因 $\\tan x - \\sin x = \\tan x (1 - \\cos x) \\sim x \\cdot \\frac{1}{2} x^2 = \\frac{1}{2} x^3$，故原式 $= \\lim_{x \\to 0} \\frac{\\frac{1}{2} x^3}{x^3} = \\frac{1}{2}$。";
        }
    }

    private String getMockBlocksJson(int pageNo) {
        return "[{\"id\":1,\"bbox\":[40,60,520,110],\"text\":\"第一章：极限论基础与无穷小分析\",\"confidence\":0.99}," +
                "{\"id\":2,\"bbox\":[40,130,520,240],\"text\":\"定义 1.1（极限的存在准则）\",\"confidence\":0.98}]";
    }
}
