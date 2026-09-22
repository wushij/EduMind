package com.edumind.ai.rag.model;

import org.springframework.util.StringUtils;

/**
 * RAG 诊断工作台的 LLM 覆盖参数。
 *
 * <p>三者均为可选项：</p>
 * <ul>
 *   <li>{@code modelKey}：用户显式选择的模型配置键。调用方必须先经模型治理策略
 *       （白名单 / 自选开关）校验，未通过时传 null，由网关回落「场景路由 → 平台默认」；</li>
 *   <li>{@code temperature}：仅用于诊断的温度覆盖值，null 表示沿用模型配置里的默认温度；</li>
 *   <li>{@code systemPrompt}：自定义系统提示词，空表示使用平台默认的 chat 系统提示词。</li>
 * </ul>
 *
 * <p>注意：该对象只服务于诊断链路，正式对话链路仍按平台策略解析模型与提示词，
 * 避免诊断参数外溢影响生产行为。</p>
 */
public record RagDebugOptions(String modelKey, Double temperature, String systemPrompt) {

    private static final int MAX_SYSTEM_PROMPT_LENGTH = 4000;

    public static RagDebugOptions none() {
        return new RagDebugOptions(null, null, null);
    }

    /** 系统提示词覆盖值（去掉首尾空白；空白视为未覆盖） */
    public String effectiveSystemPrompt() {
        if (!StringUtils.hasText(systemPrompt)) {
            return null;
        }
        String trimmed = systemPrompt.trim();
        return trimmed.length() > MAX_SYSTEM_PROMPT_LENGTH
                ? trimmed.substring(0, MAX_SYSTEM_PROMPT_LENGTH)
                : trimmed;
    }

    /** 温度覆盖值：非有限值或超出 [0,2] 时一律忽略，避免把非法值透传给模型 SDK */
    public Double effectiveTemperature() {
        if (temperature == null || temperature.isNaN() || temperature.isInfinite()) {
            return null;
        }
        if (temperature < 0D || temperature > 2D) {
            return null;
        }
        return temperature;
    }

    public boolean hasTemperatureOverride() {
        return effectiveTemperature() != null;
    }
}
