package com.edumind.knowledge.vo.knowledge;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class IndexStatusVO {
    private String status;
    private Integer totalChunks;
    private Integer indexedChunks;
    private Integer failedChunks;
    private String embeddingModel;
    /** true=当前向量来自 Mock（未接入真实 Embedding Key），检索结果不可信 */
    private Boolean embeddingMocked;
    private Integer dimensions;
    private String engine;
    private String engineVersion;
    private String connectionStatus;
    private String collectionName;
    private String indexType;
    private String metricType;
    private Long avgQueryLatencyMs;
    private LocalDateTime startedAt;
    private List<IndexErrorVO> errors;

    @Data
    public static class IndexErrorVO {
        private Long chunkId;
        private Integer chunkIndex;
        private Long documentId;
        private String documentName;
        private String snippet;
        private String errorCode;
        private String message;
        private Integer retryCount;
        private LocalDateTime failedAt;
    }
}
