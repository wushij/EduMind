package com.edumind.course.dto.course;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

@Data
public class CourseJoinDTO implements Serializable {

    @NotBlank(message = "课程代码或邀请码不能为空")
    private String code;
}
