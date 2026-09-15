package com.edumind.course.dto.course;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

@Data
public class CourseCreateDTO implements Serializable {

    @NotBlank(message = "课程名称不能为空")
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
    private java.util.List<String> initialChapters;
}
