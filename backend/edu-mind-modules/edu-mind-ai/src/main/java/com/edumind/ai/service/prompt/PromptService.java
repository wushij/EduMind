package com.edumind.ai.service.prompt;

import com.edumind.ai.dao.PromptTemplateDao;
import com.edumind.ai.entity.PromptTemplateEntity;
import com.edumind.ai.prompt.AiPromptConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class PromptService {

    private final PromptTemplateDao promptTemplateDao;
    private final Map<String, String> cache = new ConcurrentHashMap<>();

    public String getSystemPrompt(String templateName) {
        return cache.computeIfAbsent(templateName, this::loadTemplate);
    }

    public String renderTemplate(String templateName, Map<String, String> variables) {
        String template = getSystemPrompt(templateName);
        if (variables == null || variables.isEmpty()) {
            return template;
        }
        String rendered = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            rendered = rendered.replace("{{" + entry.getKey() + "}}", entry.getValue() != null ? entry.getValue() : "");
        }
        return rendered;
    }

    /** 发布/回滚后清除运行时缓存，确保 Chat/RAG 立即加载新版本 */
    public void evictTemplate(String templateName) {
        if (templateName != null) {
            cache.remove(templateName);
        }
    }

    private String loadTemplate(String templateName) {
        PromptTemplateEntity published = promptTemplateDao.findByCode(templateName);
        if (published != null && "PUBLISHED".equals(published.getStatus())) {
            return published.getContent();
        }
        String path = "prompt/" + templateName + ".st";
        try {
            ClassPathResource resource = new ClassPathResource(path);
            if (resource.exists()) {
                return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8).trim();
            }
        } catch (IOException ignored) {
            // fallback below
        }
        return fallback(templateName);
    }

    private String fallback(String templateName) {
        return switch (templateName) {
            case "question_generate" -> AiPromptConstants.QUESTION_GENERATE_SYSTEM;
            case "chat" -> AiPromptConstants.CHAT_SYSTEM;
            case "subjective_grading" -> AiPromptConstants.SUBJECTIVE_GRADING_SYSTEM;
            case "global_assistant" -> AiPromptConstants.GLOBAL_ASSISTANT_SYSTEM;
            case "navigate" -> AiPromptConstants.NAVIGATE_SYSTEM;
            default -> "";
        };
    }
}
