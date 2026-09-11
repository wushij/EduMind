package com.edumind.statistics.vo.learning;

import lombok.Data;

@Data
public class RecommendedResourceVO {
    private Long id;
    private String title;
    private String resourceType;
    private String fileUrl;
    private Long courseId;
    private Long chapterId;
}
