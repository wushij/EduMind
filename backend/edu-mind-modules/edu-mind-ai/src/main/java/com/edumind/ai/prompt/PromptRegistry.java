package com.edumind.ai.prompt;

import java.util.Map;

/**
 * 统一 Prompt 模板管理注册中心
 * 严禁在 Service 中散落硬编码 Prompt 字符串
 */
public interface PromptRegistry {
    String getPrompt(String templateKey, Map<String, Object> variables);
}
