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
    private LocalDateTime createTime;
}
