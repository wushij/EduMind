package com.edumind.resource.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CourseResourceVO {
    private Long id;
    private Long courseId;
    private Long resourceId;
    private Long documentId;
    private String title;
    private String resourceType;
    private Long chapterId;
    private String downloadUrl;
    /** 关联知识库文档解析状态（PENDING/PARSING/SUCCESS/FAILED） */
    private String knowledgeParseStatus;
    private Integer knowledgeChunkCount;
    private LocalDateTime createTime;
}
