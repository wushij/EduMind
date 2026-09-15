package com.edumind.course.dto.course;

import lombok.Data;

import java.io.Serializable;

@Data
public class CourseUpdateDTO implements Serializable {

    private String name;
    private String description;
    private String coverUrl;
    private String semester;
    private String code;
    private String category;
    private java.math.BigDecimal credits;
    private Integer plannedHours;
    private Long knowledgeBaseId;
    private String aiPersona;
    private String welcomeMessage;
    private String status;
}
