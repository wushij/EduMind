package com.edumind.ai.vo.rag;

import lombok.Data;

import java.util.List;

@Data
public class RagDebugResponseVO {
    private String originalQuery;
    private String rewrittenQuery;
    private List<RetrievalResultVO> retrievalResults;
    private String context;
    private String promptPreview;
    private String answer;

    /** 各阶段真实耗时（含跳过/失败状态） */
    private List<RagStageTimingVO> stageTimings;
    /** 实际发起生成调用的模型配置键；跳过生成时为 null */
    private String modelKey;
    /** 本次是否使用了调用方覆盖的系统提示词 */
    private Boolean systemPromptOverridden;
    /** Token 估算量（口径与 AI 调用审计一致：字符数 / 4） */
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer totalTokens;
    /** Pipeline 端到端耗时（毫秒） */
    private Long totalLatencyMs;
}
