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
            return "### 一、选择题（本大题共 3 小题，每小题 5 分，共 15 分）\n\n" +
                    "**1. 导数与单调性**\n" +
                    "已知函数 $f(x) = \\frac{\\ln x}{x} + \\frac{1}{2}ax^2$，若 $f(x)$ 在区间 $(1, +\\infty)$ 内单调递减，则实数 $a$ 的取值范围是（   ）\n" +
                    "A. $(-\\infty, -1]$\n" +
                    "B. $(-\\infty, 0]$\n" +
                    "C. $[1, +\\infty)$\n" +
                    "D. $(0, 1]$\n\n" +
                    "**2. 复数代数运算**\n" +
                    "设复数 $z$ 满足 $(1 + i)z = 2 - i$，则 $|z| = $（   ）\n" +
                    "A. $\\frac{\\sqrt{10}}{2}$\n" +
                    "B. $\\frac{5}{2}$\n" +
                    "C. $\\sqrt{5}$\n" +
                    "D. $\\frac{\\sqrt{5}}{2}$\n\n" +
                    "**3. 空间立体几何**\n" +
                    "在正三棱柱 $ABC-A_1B_1C_1$ 中，若各棱长均为 $2$，则异面直线 $AB_1$ 与 $BC_1$ 所成角的余弦值为（   ）\n" +
                    "A. $\\frac{1}{4}$\n" +
                    "B. $\\frac{\\sqrt{3}}{4}$\n" +
                    "C. $\\frac{1}{2}$\n" +
                    "D. $\\frac{\\sqrt{2}}{2}$";
        } else if (pageNo == 2) {
            return "### 二、填空题与解答题（本大题共 3 小题）\n\n" +
                    "**4. 二项式展开定理**\n" +
                    "在 $(x - \\frac{2}{x})^6$ 的二项展开式中，常数项为 ________。\n\n" +
                    "**5. 双曲线的几何性质**\n" +
                    "已知双曲线 $C: \\frac{x^2}{a^2} - \\frac{y^2}{b^2} = 1 (a > 0, b > 0)$ 的一条渐近线方程为 $y = \\sqrt{3}x$，则其离心率 $e = $ ________。\n\n" +
                    "**6. 解三角形综合应用（本小题满分 12 分）**\n" +
                    "在 $\\triangle ABC$ 中，角 $A, B, C$ 所对的边分别为 $a, b, c$，已知 $2a\\sin B = \\sqrt{3}b$。\n" +
                    "(1) 求角 $A$ 的大小；\n" +
                    "(2) 若 $a = \\sqrt{7}$，$b + c = 5$，求 $\\triangle ABC$ 的面积。";
        } else {
            return "### 三、压轴解答题（本小题满分 12 分）\n\n" +
                    "**7. 解析几何与椭圆方程**\n" +
                    "已知椭圆 $C: \\frac{x^2}{a^2} + \\frac{y^2}{b^2} = 1 (a > b > 0)$ 的离心率为 $\\frac{\\sqrt{3}}{2}$，左焦点为 $F_1(-c, 0)$，短轴长为 $2$。\n" +
                    "(1) 求椭圆 $C$ 的标准方程；\n" +
                    "(2) 设直线 $l: y = kx + m$ 与椭圆 $C$ 交于不同的两点 $A, B$，以 $AB$ 为直径的圆恰好过原点 $O$，求原点 $O$ 到直线 $l$ 的距离的取值范围。";
        }
    }

    private String getMockBlocksJson(int pageNo) {
        if (pageNo == 1) {
            return "[{\"id\":1,\"bbox\":[30,120,540,240],\"title\":\"Q1 · 导数与单调性\",\"confidence\":0.992}," +
                    "{\"id\":2,\"bbox\":[30,260,540,380],\"title\":\"Q2 · 复数代数运算\",\"confidence\":0.987}," +
                    "{\"id\":3,\"bbox\":[30,400,540,510],\"title\":\"Q3 · 空间立体几何\",\"confidence\":0.975}]";
        } else if (pageNo == 2) {
            return "[{\"id\":4,\"bbox\":[30,100,540,190],\"title\":\"Q4 · 二项式展开\",\"confidence\":0.988}," +
                    "{\"id\":5,\"bbox\":[30,210,540,300],\"title\":\"Q5 · 双曲线离心率\",\"confidence\":0.981}," +
                    "{\"id\":6,\"bbox\":[30,320,540,520],\"title\":\"Q6 · 解三角形综合\",\"confidence\":0.979}]";
        } else {
            return "[{\"id\":7,\"bbox\":[30,100,540,480],\"title\":\"Q7 · 解析几何与椭圆\",\"confidence\":0.985}]";
        }
    }
}
