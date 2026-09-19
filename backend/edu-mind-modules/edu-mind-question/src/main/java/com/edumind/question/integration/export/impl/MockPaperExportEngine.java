package com.edumind.question.integration.export.impl;

import com.edumind.common.exception.BusinessException;
import com.edumind.question.integration.export.PaperExportEngine;
import com.edumind.question.integration.export.PaperExportRequest;
import com.edumind.question.integration.export.PaperExportResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * 试卷导出引擎 Mock 适配实现（Beta 阶段默认，生成包含排版与归属断言特征的文档流）
 */
@Slf4j
@Primary
@Profile("test")
@Component
public class MockPaperExportEngine implements PaperExportEngine {

    private static volatile boolean forceFail = false;
    private static volatile long mockDelayMs = 200;

    public static void setForceFail(boolean fail) {
        forceFail = fail;
    }

    public static void setMockDelayMs(long delayMs) {
        mockDelayMs = delayMs;
    }

    @Override
    public PaperExportResult export(PaperExportRequest request) {
        if (mockDelayMs > 0) {
            try {
                Thread.sleep(mockDelayMs);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
        if (forceFail) {
            throw new BusinessException("Mock paper export engine forced failure");
        }

        String content = String.format(
                "%%PDF-1.4 Mock Exam Paper Document\n" +
                "TaskId: %s\n" +
                "TenantId: %s\n" +
                "UserId: %s\n" +
                "ExamId: %s\n" +
                "PaperTitle: %s\n" +
                "PaperSubtitle: %s\n" +
                "PaperSize: %s\n" +
                "ExportParams: %s\n" +
                "%%EOF",
                request.getTaskId(),
                request.getTenantId(),
                request.getUserId(),
                request.getExamId(),
                request.getPaperTitle() != null ? request.getPaperTitle() : "Default Paper",
                request.getPaperSubtitle() != null ? request.getPaperSubtitle() : "",
                request.getPaperSize() != null ? request.getPaperSize() : "A4",
                request.getExportParamsJson() != null ? request.getExportParamsJson() : "{}"
        );

        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        String filename = "exam_" + request.getExamId() + "_" + request.getTaskId() + ".pdf";

        return PaperExportResult.builder()
                .fileBytes(bytes)
                .filename(filename)
                .contentType("application/pdf")
                .build();
    }
}
