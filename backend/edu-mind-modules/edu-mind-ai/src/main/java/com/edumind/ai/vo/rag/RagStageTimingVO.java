package com.edumind.ai.vo.rag;

import lombok.Data;

/** RAG Pipeline 单阶段耗时 VO（诊断工作台时序面板） */
@Data
public class RagStageTimingVO {
    private String stage;
    private String stageName;
    private Long durationMs;
    private String status;
    private String summary;
}
