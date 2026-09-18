package com.edumind.knowledge.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("knowledge_document")
public class KnowledgeDocumentEntity implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long knowledgeBaseId;
    /** UPLOAD | LESSON */
    private String sourceType;
    private Long courseId;
    private Long lessonChapterId;
    private String contentHash;
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
