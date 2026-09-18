package com.edumind.knowledge.vo.rag;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class KnowledgeRagDashboardVO {

    private long totalDocuments;
    private long indexedDocuments;
    private long lessonDocuments;
    private long uploadDocuments;
    private long totalChunks;
    private long lessonChunks;
    private long uploadChunks;
    private long indexedChunks;
    private long failedChunks;
    private long normalChunks;
    private long orphanChunkEstimate;
    private long totalSessions;
    private Integer embeddingDimension;
    private String embeddingModelName;
    private boolean hybridEnabled;
    private String retrievalModelDescription;
    /** 当前运行时向量存储引擎（Milvus / InMemory 等） */
    private String vectorStoreLabel;
    /** 已向量化切片健康占比（0–100，基于 normal/indexed） */
    private Double indexHealthPercent;
    private Double minRrfScore;
    private String latestIndexStatus;
    private Integer latestIndexTotalChunks;
    private Integer latestIndexIndexedChunks;
    private Integer latestIndexFailedChunks;
    private String latestIndexEmbeddingModel;
    private LocalDateTime latestIndexStartedAt;
}
