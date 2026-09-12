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
    private LocalDateTime startedAt;
    private List<IndexErrorVO> errors;

    @Data
    public static class IndexErrorVO {
        private Long chunkId;
        private String message;
    }
}
