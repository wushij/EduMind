package com.edumind.ai.vo.rag;

import lombok.Data;

@Data
public class CitationVO {
    private String documentName;
    private Integer pageNo;
    private Long chunkId;
    private Double score;
    private String excerpt;
    private Integer chunkIndex;
    private Long lessonChapterId;
    private String anchor;
    private Long documentId;
    private Long knowledgeBaseId;
}
