package com.edumind.knowledge.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("knowledge_chunk_index")
public class KnowledgeChunkIndexEntity implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long chunkId;
    private Long knowledgeBaseId;
    private Long documentId;
    private String vectorId;
    private String embedStatus;
    private String embeddingModel;
    private String errorMessage;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
