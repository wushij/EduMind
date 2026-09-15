package com.edumind.boot.ga;

import com.edumind.ai.integration.llm.LlmProperties;
import com.edumind.knowledge.config.KnowledgeOcrProperties;
import com.edumind.knowledge.integration.ocr.OcrEngineAdapter;
import com.edumind.knowledge.integration.ocr.impl.MockOcrEngineAdapter;
import com.edumind.knowledge.integration.ocr.impl.PaddleOcrEngineAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;

/**
 * GA 生产环境启动校验：禁止 Mock LLM/OCR 静默降级
 */
@Component
public class GaProductionStartupValidator {

    private static final Logger log = LoggerFactory.getLogger(GaProductionStartupValidator.class);

    private final Environment environment;
    private final ApplicationContext applicationContext;
    private final LlmProperties llmProperties;
    private final KnowledgeOcrProperties knowledgeOcrProperties;

    public GaProductionStartupValidator(Environment environment,
                                        ApplicationContext applicationContext,
                                        LlmProperties llmProperties,
                                        KnowledgeOcrProperties knowledgeOcrProperties) {
        this.environment = environment;
        this.applicationContext = applicationContext;
        this.llmProperties = llmProperties;
        this.knowledgeOcrProperties = knowledgeOcrProperties;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void validateProductionProfile() {
        if (!Arrays.asList(environment.getActiveProfiles()).contains("prod")) {
            return;
        }

        if (Boolean.TRUE.equals(llmProperties.getMockEnabled())) {
            throw new IllegalStateException("[GA] prod profile 禁止 ai.llm.mock-enabled=true");
        }
        if ("mock".equalsIgnoreCase(llmProperties.getProvider())) {
            throw new IllegalStateException("[GA] prod profile 禁止 ai.llm.provider=mock");
        }
        if (!StringUtils.hasText(llmProperties.getApiKey())) {
            log.warn("[GA] ai.llm.apiKey 未配置，将依赖 ai_model_config KMS 密文；请确认至少一条 chat 模型已启用");
        }

        if (Boolean.TRUE.equals(knowledgeOcrProperties.getMockEnabled())) {
            throw new IllegalStateException("[GA] prod profile 禁止 knowledge.ocr.mock-enabled=true");
        }
        if (!"paddle".equalsIgnoreCase(knowledgeOcrProperties.getEngine())) {
            throw new IllegalStateException("[GA] prod profile 要求 knowledge.ocr.engine=paddle");
        }
        if (!StringUtils.hasText(knowledgeOcrProperties.getPaddle().getBaseUrl())) {
            throw new IllegalStateException("[GA] prod profile 要求配置 knowledge.ocr.paddle.base-url (PADDLE_OCR_BASE_URL)");
        }

        OcrEngineAdapter active = applicationContext.getBean(OcrEngineAdapter.class);
        if (active instanceof MockOcrEngineAdapter) {
            throw new IllegalStateException("[GA] prod 环境检测到 MockOcrEngineAdapter 为 Primary，拒绝启动");
        }
        if (!(active instanceof PaddleOcrEngineAdapter)) {
            throw new IllegalStateException("[GA] prod OCR 引擎必须为 PaddleOcrEngineAdapter，当前: " + active.getClass().getSimpleName());
        }

        log.info("[GA] Production startup validation passed (LLM mock disabled, Paddle OCR configured)");
    }
}
