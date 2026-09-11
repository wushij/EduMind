package com.edumind.ai.service.prompt;

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

    private final Map<String, String> cache = new ConcurrentHashMap<>();

    public String getSystemPrompt(String templateName) {
        return cache.computeIfAbsent(templateName, this::loadTemplate);
    }

    private String loadTemplate(String templateName) {
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
            default -> "";
        };
    }
}
