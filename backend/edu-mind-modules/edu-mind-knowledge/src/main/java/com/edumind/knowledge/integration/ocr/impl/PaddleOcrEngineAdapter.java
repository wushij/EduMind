package com.edumind.knowledge.integration.ocr.impl;

import com.edumind.knowledge.config.KnowledgeOcrProperties;
import com.edumind.knowledge.integration.ocr.OcrEngineAdapter;
import com.edumind.knowledge.integration.ocr.OcrPageResult;
import com.edumind.knowledge.integration.ocr.OcrRecognizeRequest;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * PaddleOCR HTTP 引擎适配器（对接 sidecar / 独立 OCR 服务）
 */
@Slf4j
@Primary
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "knowledge.ocr", name = "mock-enabled", havingValue = "false")
public class PaddleOcrEngineAdapter implements OcrEngineAdapter {

    private final KnowledgeOcrProperties ocrProperties;
    private final ObjectMapper objectMapper;
    private final RestClient.Builder restClientBuilder;

    @Override
    public String getEngineCode() {
        return "PADDLE_OCR";
    }

    @Override
    public boolean supports(String engine) {
        return !StringUtils.hasText(engine) || "PADDLE_OCR".equalsIgnoreCase(engine);
    }

    @Override
    public List<OcrPageResult> recognize(OcrRecognizeRequest request) {
        String baseUrl = ocrProperties.getPaddle().getBaseUrl();
        if (!StringUtils.hasText(baseUrl)) {
            throw new IllegalStateException("Paddle OCR base-url is not configured");
        }

        RestClient client = restClientBuilder
                .baseUrl(baseUrl.replaceAll("/+$", ""))
                .build();

        Map<String, Object> body = Map.of(
                "taskId", request.getTaskId(),
                "documentId", request.getDocumentId(),
                "tenantId", request.getTenantId(),
                "engine", StringUtils.hasText(request.getEngine()) ? request.getEngine() : "PADDLE_OCR"
        );

        log.info("[PaddleOcrEngine] Calling OCR service taskId={}, documentId={}", request.getTaskId(), request.getDocumentId());

        String responseBody = client.post()
                .uri("/api/v1/ocr/recognize")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(String.class);

        return parsePages(responseBody);
    }

    private List<OcrPageResult> parsePages(String responseBody) {
        if (!StringUtils.hasText(responseBody)) {
            throw new IllegalStateException("Paddle OCR service returned empty body");
        }
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode pagesNode = root.has("pages") ? root.get("pages") : root;
            if (!pagesNode.isArray()) {
                throw new IllegalStateException("Paddle OCR response missing pages array");
            }
            List<OcrPageResult> results = objectMapper.convertValue(pagesNode, new TypeReference<>() {});
            if (results == null || results.isEmpty()) {
                throw new IllegalStateException("Paddle OCR returned zero pages");
            }
            return results;
        } catch (IllegalStateException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("[PaddleOcrEngine] Failed to parse OCR response: {}", ex.getMessage());
            throw new IllegalStateException("Invalid Paddle OCR response: " + ex.getMessage(), ex);
        }
    }
}
