package com.edumind.knowledge.vo.knowledge;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class KnowledgeDocumentVO {
    private Long id;
    private Long knowledgeBaseId;
    private String fileName;
    private String fileType;
    private Long fileSize;
    private String objectKey;
    private String parseStatus;
    private String errorMessage;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
