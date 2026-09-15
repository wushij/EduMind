package com.edumind.resource.dto.course;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

@Data
public class CourseResourceCreateDTO implements Serializable {

    private Long courseId;
    private Long chapterId;
    private Long resourceId;
    private Long documentId;

    @NotBlank(message = "资源名称不能为空")
    private String title;

    private String resourceType;
}
