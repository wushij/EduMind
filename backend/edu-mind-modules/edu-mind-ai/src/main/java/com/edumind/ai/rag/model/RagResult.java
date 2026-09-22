package com.edumind.ai.rag.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class RagResult {
    private String originalQuery;
    private String rewrittenQuery;
    private List<RetrievalHit> retrievalResults;
    private String context;
    private String promptPreview;
    private String answer;

    /** 各阶段真实耗时（诊断工作台时序面板） */
    private List<RagStageTiming> stageTimings;
    /** 实际发起生成调用的模型配置键（可能因回落而与请求值不同） */
    private String modelKey;
    /** 真实使用的系统提示词是否来自调用方覆盖 */
    private Boolean systemPromptOverridden;
    /** Prompt Token 估算量 */
    private Integer promptTokens;
    /** 生成 Token 估算量 */
    private Integer completionTokens;
    /** 总 Token 估算量 */
    private Integer totalTokens;
    /** Pipeline 端到端耗时（毫秒） */
    private Long totalLatencyMs;
}
