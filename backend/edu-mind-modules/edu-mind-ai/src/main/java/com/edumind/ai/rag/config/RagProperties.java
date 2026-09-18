package com.edumind.ai.rag.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "edumind.rag")
public class RagProperties {

    /** 向量 + 关键词多路召回 + RRF（对齐 Code Compass HybridSearch） */
    private boolean hybridEnabled = true;

    /** RRF 融合分下限（与 Compass MinRAGCitationRRFScore 一致量级） */
    private double minRrfScore = 0.03;

    private double rrfK = 60.0;

    /** 英文技术词 AND 分支权重 */
    private double techTermBranchWeight = 2.0;
}
