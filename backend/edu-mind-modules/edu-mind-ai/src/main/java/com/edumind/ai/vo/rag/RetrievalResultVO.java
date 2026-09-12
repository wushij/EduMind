package com.edumind.ai.vo.rag;

import lombok.Data;

import java.util.Map;

@Data
public class RetrievalResultVO {
    private Long chunkId;
    private Double score;
    private Long documentId;
    private String documentName;
    private Integer pageNo;
    private String excerpt;
    private Map<String, Object> metadata;
}
