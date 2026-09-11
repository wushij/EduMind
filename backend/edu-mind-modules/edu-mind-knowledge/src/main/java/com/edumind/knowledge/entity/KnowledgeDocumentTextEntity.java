package com.edumind.knowledge.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("knowledge_document_text")
public class KnowledgeDocumentTextEntity implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long documentId;
    private String content;
    private LocalDateTime createTime;
}
