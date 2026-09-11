package com.edumind.course.dto.course;

import lombok.Data;

import java.io.Serializable;

@Data
public class CourseQueryDTO implements Serializable {

    private String keyword;
    private String status;
    private Long page;
    private Long pageSize;
}
