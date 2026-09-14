package com.edumind.ai.rag.query;

import com.edumind.ai.dao.PromptTemplateDao;
import com.edumind.ai.entity.PromptTemplateEntity;
import com.edumind.ai.gateway.ModelRouter;
import com.edumind.ai.integration.llm.LlmChatOptions;
import com.edumind.ai.integration.llm.LlmClient;
import com.edumind.ai.integration.llm.LlmClientRegistry;
import com.edumind.ai.integration.llm.MockLlmClient;
import com.edumind.ai.prompt.AiPromptConstants;
import com.edumind.ai.service.prompt.PromptService;
import com.edumind.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class QueryRewriter {

    private final PromptService promptService;
    private final PromptTemplateDao promptTemplateDao;
    private final ModelRouter modelRouter;
    private final LlmClientRegistry llmClientRegistry;

    public String rewrite(String query) {
        return rewrite(QueryRewriteContext.ofQuestion(query));
    }

    public String rewrite(QueryRewriteContext context) {
        if (context == null || !context.hasQuestion()) {
            return normalizeQuery(context != null ? context.getQuestion() : null);
        }

        PromptTemplateEntity template = promptTemplateDao.findByCode(AiPromptConstants.COURSE_RAG_QUERY_REWRITE);
        if (template == null || !"PUBLISHED".equalsIgnoreCase(template.getStatus())) {
            return normalizeQuery(context.getQuestion());
        }

        Map<String, String> vars = context.toVariables();
        String systemPrompt = promptService.renderSystemPrompt(AiPromptConstants.COURSE_RAG_QUERY_REWRITE, vars);
        String userPrompt = promptService.renderUserContent(AiPromptConstants.COURSE_RAG_QUERY_REWRITE, vars);
        if (!StringUtils.hasText(systemPrompt) && !StringUtils.hasText(userPrompt)) {
            return normalizeQuery(context.getQuestion());
        }

        try {
            String modelKey = modelRouter.resolveModelKey("CHAT", template.getBoundModel());
            LlmClient client = llmClientRegistry.get(modelKey);
            if (client instanceof MockLlmClient && !"mock".equalsIgnoreCase(modelKey)) {
                throw new BusinessException("Query 改写模型不可用");
            }
            LlmChatOptions options = LlmChatOptions.forQueryRewrite(
                    resolveTemperature(template, null),
                    resolveMaxTokens(template, null)
            );
            String raw = client.chat(systemPrompt, userPrompt, options);
            return sanitizeRewriteOutput(raw, context.getQuestion(), context.getCourseName());
        } catch (Exception ex) {
            log.warn("Query rewrite failed, fallback to original query: {}", ex.getMessage());
            return normalizeQuery(context.getQuestion());
        }
    }

    static String sanitizeRewriteOutput(String raw, String original, String courseName) {
        if (!StringUtils.hasText(raw)) {
            return normalizeQuery(original);
        }

        String cleaned = raw.trim()
                .replaceAll("(?s)```[\\w-]*\\n?", "")
                .replace("```", "")
                .trim();
        cleaned = Arrays.stream(cleaned.split("\\R"))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .filter(line -> !line.startsWith("```"))
                .findFirst()
                .orElse(cleaned);

        if ((cleaned.startsWith("\"") && cleaned.endsWith("\""))
                || (cleaned.startsWith("「") && cleaned.endsWith("」"))
                || (cleaned.startsWith("『") && cleaned.endsWith("』"))) {
            cleaned = cleaned.substring(1, cleaned.length() - 1).trim();
        }

        if (!StringUtils.hasText(cleaned)) {
            return normalizeQuery(original);
        }

        String normalizedOriginal = normalizeQuery(original);
        String normalizedCleaned = normalizeQuery(cleaned);
        if (normalizedCleaned.equals(normalizedOriginal) && StringUtils.hasText(courseName)
                && !normalizedCleaned.contains(courseName.trim())) {
            return courseName.trim() + " " + cleaned;
        }
        return cleaned;
    }

    private static String normalizeQuery(String query) {
        if (!StringUtils.hasText(query)) {
            return query;
        }
        return query.trim().replaceAll("\\s+", " ");
    }

    private static Double resolveTemperature(PromptTemplateEntity entity, Double override) {
        if (override != null) {
            return override;
        }
        BigDecimal value = entity.getTemperature();
        return value != null ? value.doubleValue() : 0.1D;
    }

    private static Integer resolveMaxTokens(PromptTemplateEntity entity, Integer override) {
        if (override != null && override > 0) {
            return override;
        }
        Integer value = entity.getMaxTokens();
        return value != null && value > 0 ? value : 512;
    }
}
