package com.edumind.course.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CourseBriefVO {
    private Long id;
    private String title;
    private String code;
    private LocalDateTime lastVisitAt;
}
