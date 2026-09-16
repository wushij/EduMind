package com.edumind.resource.vo;

import lombok.Data;

@Data
public class ResourceVO {
    private Long id;
    private Long courseId;
    private Long chapterId;
    private String title;
    private String resourceType;
    private String fileUrl;
    private String description;
}
