package com.edumind.knowledge.vo.knowledge;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChunkVO {
    private Long id;
    private Long documentId;
    private Long knowledgeBaseId;
    private Integer chunkIndex;
    private String content;
    private Integer pageNo;
    private String heading;
    private Integer charCount;
    private Integer tokenEstimate;
    private LocalDateTime createTime;
}
