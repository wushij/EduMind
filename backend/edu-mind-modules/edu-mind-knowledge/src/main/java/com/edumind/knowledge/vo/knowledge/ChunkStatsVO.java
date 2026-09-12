package com.edumind.knowledge.vo.knowledge;

import lombok.Data;

@Data
public class ChunkStatsVO {
    private Long totalChunks;
    private Long indexedChunks;
    private Long pendingChunks;
    private Long failedChunks;
    private Integer avgTokens;
    private Long totalTokens;
}
