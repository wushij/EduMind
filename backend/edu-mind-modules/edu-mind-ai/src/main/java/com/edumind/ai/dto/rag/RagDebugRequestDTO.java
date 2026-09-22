package com.edumind.ai.dto.rag;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class RagDebugRequestDTO extends RetrievalRequestDTO {
    private Long knowledgeBaseId;
    private Boolean debugMode = true;
    private Boolean skipLlm = false;

    /**
     * 显式指定的模型配置键（诊断用用户自选模型）。
     * <p>必须经 {@code AiUserModelPolicy} 白名单校验后才允许生效，未通过时回落平台策略。</p>
     */
    private String modelKey;
    /** 生成温度覆盖值，null 表示沿用模型配置的默认温度 */
    private Double temperature;
    /** 系统提示词覆盖值，空表示使用平台默认的 chat 系统提示词 */
    private String systemPrompt;
}
