package com.edumind.ai.rag.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "edumind.rag")
public class RagProperties {

    /** 向量 + 关键词多路召回 + RRF（对齐 Code Compass HybridSearch） */
    private boolean hybridEnabled = true;

    /** RRF 融合分下限：单路命中首位约为 0.01639，下限设为 0.005 保证单路前列切片不被清空 */
    private double minRrfScore = 0.005;

    private double rrfK = 60.0;

    /** 英文技术词 AND 分支权重 */
    private double techTermBranchWeight = 2.0;
}
