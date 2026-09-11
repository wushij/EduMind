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
}
