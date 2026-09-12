package com.edumind.knowledge.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("knowledge_document_chunk")
public class KnowledgeDocumentChunkEntity implements Serializable {
    @TableId(type = IdType.AUTO)
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
