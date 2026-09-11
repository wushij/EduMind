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
    private String status;
}
