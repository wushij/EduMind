package com.edumind.question.integration.export.impl;

import com.edumind.common.exception.BusinessException;
import com.edumind.question.dto.export.PaperExportRequestDTO;
import com.edumind.question.integration.export.PaperExportEngine;
import com.edumind.question.integration.export.PaperExportRequest;
import com.edumind.question.integration.export.PaperExportResult;
import com.edumind.question.integration.export.support.PaperExportSectionGrouper;
import com.edumind.question.service.export.PaperExportHtmlRenderer;
import com.edumind.teaching.api.ExamQueryApi;
import com.edumind.teaching.vo.exam.ExamVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.edumind.question.integration.export.support.OpenHtmlPdfFontRegistrar;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@Primary
@Profile("!test")
@Component
@RequiredArgsConstructor
public class HtmlPaperExportEngine implements PaperExportEngine {

    static {
        System.setProperty("java.awt.headless", "true");
    }

    private static final long PDF_RENDER_TIMEOUT_SECONDS = 180;

    private final ExamQueryApi examQueryApi;
    private final PaperExportHtmlRenderer htmlRenderer;
    private final ObjectMapper objectMapper;

    @Override
    public PaperExportResult export(PaperExportRequest request) {
        if (request.getExamId() == null) {
            throw new BusinessException("试卷 ID 不能为空");
        }
        ExamVO exam = examQueryApi.getExamById(request.getExamId());
        if (exam == null) {
            throw new BusinessException("试卷不存在");
        }
        if (PaperExportSectionGrouper.group(exam).isEmpty()) {
            throw new BusinessException("试卷暂无题目，无法导出");
        }

        PaperExportRequestDTO params = parseParams(request.getExportParamsJson());
        if (!StringUtils.hasText(params.getPaperTitle())) {
            params.setPaperTitle(exam.getTitle());
        }

        String html = htmlRenderer.render(params, exam);
        byte[] pdfBytes = renderPdf(html);

        String filename = "exam_" + request.getExamId() + "_" + request.getTaskId() + ".pdf";
        return PaperExportResult.builder()
                .fileBytes(pdfBytes)
                .filename(filename)
                .contentType("application/pdf")
                .build();
    }

    private PaperExportRequestDTO parseParams(String json) {
        if (!StringUtils.hasText(json)) {
            return new PaperExportRequestDTO();
        }
        try {
            return objectMapper.readValue(json, PaperExportRequestDTO.class);
        } catch (Exception e) {
            log.warn("[HtmlPaperExportEngine] 解析 exportParams 失败，使用默认排版参数: {}", e.getMessage());
            return new PaperExportRequestDTO();
        }
    }

    private byte[] renderPdf(String html) {
        ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "openhtml-pdf-render");
            t.setDaemon(true);
            return t;
        });
        try {
            Future<byte[]> future = executor.submit((Callable<byte[]>) () -> renderPdfInternal(html));
            return future.get(PDF_RENDER_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            throw new BusinessException("PDF 排版渲染超时，请稍后重试或简化卷面选项");
        } catch (Exception e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            log.error("[HtmlPaperExportEngine] PDF render failed", cause);
            String detail = cause.getMessage() != null ? cause.getMessage() : cause.getClass().getSimpleName();
            if (detail.contains("COURIER") || detail.contains("HELVETICA") || detail.contains("TIMES")) {
                throw new BusinessException("PDF 排版失败：字体映射异常，请确认服务器已安装中文字体（如微软雅黑/宋体）后重试");
            }
            throw new BusinessException("PDF 排版渲染失败，请稍后重试或关闭答题卡/解析后重试");
        } finally {
            executor.shutdownNow();
        }
    }

    private byte[] renderPdfInternal(String html) throws Exception {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            OpenHtmlPdfFontRegistrar.register(builder);
            builder.withHtmlContent(html, null);
            builder.toStream(os);
            builder.run();
            byte[] bytes = os.toByteArray();
            if (bytes.length == 0) {
                throw new BusinessException("PDF 输出为空");
            }
            return bytes;
        }
    }

}
