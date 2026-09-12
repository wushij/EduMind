package com.edumind.ai.vo.rag;

import lombok.Data;

import java.util.List;

@Data
public class RagDebugResponseVO {
    private String originalQuery;
    private String rewrittenQuery;
    private List<RetrievalResultVO> retrievalResults;
    private String context;
    private String promptPreview;
    private String answer;
}
