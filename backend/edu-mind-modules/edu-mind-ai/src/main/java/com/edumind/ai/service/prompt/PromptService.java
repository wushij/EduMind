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
        return cache.computeIfAbsent("sys:" + templateName, k -> loadSystemPrompt(templateName));
    }

    public String renderTemplate(String templateName, Map<String, String> variables) {
        String template = cache.computeIfAbsent("content:" + templateName, k -> loadTemplateContent(templateName));
        return applyVariables(template, variables);
    }

    public String renderUserContent(String templateCode, Map<String, String> variables) {
        PromptTemplateEntity entity = promptTemplateDao.findByCode(templateCode);
        if (entity == null || !org.springframework.util.StringUtils.hasText(entity.getContent())) {
            return "";
        }
        return applyVariables(entity.getContent(), variables);
    }

    public String renderSystemPrompt(String templateCode, Map<String, String> variables) {
        PromptTemplateEntity entity = promptTemplateDao.findByCode(templateCode);
        String template;
        if (entity != null && org.springframework.util.StringUtils.hasText(entity.getSystemPrompt())) {
            template = entity.getSystemPrompt();
        } else {
            template = getSystemPrompt(templateCode);
        }
        return applyVariables(template, variables);
    }

    private String applyVariables(String template, Map<String, String> variables) {
        if (!org.springframework.util.StringUtils.hasText(template)) {
            return "";
        }
        if (variables == null || variables.isEmpty()) {
            return template;
        }
        String rendered = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            rendered = rendered.replace("{{" + entry.getKey() + "}}",
                    entry.getValue() != null ? entry.getValue() : "");
        }
        return rendered;
    }

    /** 发布/回滚后清除运行时缓存，确保 Chat/RAG 立即加载新版本 */
    public void evictTemplate(String templateName) {
        if (templateName != null) {
            cache.remove(templateName);
            cache.remove("sys:" + templateName);
            cache.remove("content:" + templateName);
        }
    }

    private String loadSystemPrompt(String templateName) {
        PromptTemplateEntity published = promptTemplateDao.findByCode(templateName);
        if (published != null && "PUBLISHED".equals(published.getStatus())) {
            if (org.springframework.util.StringUtils.hasText(published.getSystemPrompt())) {
                return published.getSystemPrompt();
            }
            return published.getContent();
        }
        return loadFromFileOrFallback(templateName);
    }

    private String loadTemplateContent(String templateName) {
        PromptTemplateEntity published = promptTemplateDao.findByCode(templateName);
        if (published != null && "PUBLISHED".equals(published.getStatus())) {
            if (org.springframework.util.StringUtils.hasText(published.getContent())) {
                return published.getContent();
            }
            return published.getSystemPrompt();
        }
        return loadFromFileOrFallback(templateName);
    }

    private String loadFromFileOrFallback(String templateName) {
        String path = "prompt/" + templateName + ".st";
        try {
            ClassPathResource resource = new ClassPathResource(path);
            if (!resource.exists()) {
                resource = new ClassPathResource("prompt/" + templateName.toLowerCase() + ".st");
            }
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
            case "question_generate", "EXAM_RAG_GENERAL", "exam_rag_general" -> AiPromptConstants.QUESTION_GENERATE_SYSTEM;
            case "chat" -> AiPromptConstants.CHAT_SYSTEM;
            case "subjective_grading", "GRADING_RAG_GENERAL", "grading_rag_general" -> AiPromptConstants.SUBJECTIVE_GRADING_SYSTEM;
            case "teaching_plan", "LESSON_PREP_RAG_GENERAL", "lesson_prep_rag_general" -> "你是高校教案备课教学设计专家，请依据教学目标设计结构化教案。";
            case "global_assistant" -> AiPromptConstants.GLOBAL_ASSISTANT_SYSTEM;
            case "navigate" -> AiPromptConstants.NAVIGATE_SYSTEM;
            default -> "";
        };
    }
}
