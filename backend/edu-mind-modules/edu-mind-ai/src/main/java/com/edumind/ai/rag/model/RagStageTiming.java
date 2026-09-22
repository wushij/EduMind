package com.edumind.ai.rag.model;

import lombok.Builder;
import lombok.Data;

/**
 * RAG Pipeline 单个阶段的真实耗时埋点（诊断工作台时序面板数据源）。
 */
@Data
@Builder
public class RagStageTiming {

    /** 阶段编码，与前端 PipelineStageTiming.stage 对齐 */
    private String stage;
    /** 阶段中文名 */
    private String stageName;
    /** 阶段耗时（毫秒） */
    private Long durationMs;
    /** SUCCESS | SKIPPED | FAILED */
    private String status;
    /** 阶段摘要：命中条数、上下文长度、使用的模型等 */
    private String summary;
}
