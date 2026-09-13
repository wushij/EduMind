package com.edumind.ai.vo;

import com.edumind.ai.vo.rag.CitationVO;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class MessageVO {
    private String id;
    private String conversationId;
    private String role;
    private String content;
    private String reasoningContent;
    private List<CitationVO> citations;
    private LocalDateTime createTime;
}
