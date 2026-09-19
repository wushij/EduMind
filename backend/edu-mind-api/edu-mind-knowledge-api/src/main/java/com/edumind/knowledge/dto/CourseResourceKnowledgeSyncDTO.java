package com.edumind.knowledge.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class CourseResourceKnowledgeSyncDTO implements Serializable {
    private Long knowledgeBaseId;
    private Long courseId;
    private Long courseResourceId;
    private String objectKey;
    private String fileName;
    /** MD / PDF / WORD / TXT 等业务类型 */
    private String resourceType;
    private Long fileSize;
}
