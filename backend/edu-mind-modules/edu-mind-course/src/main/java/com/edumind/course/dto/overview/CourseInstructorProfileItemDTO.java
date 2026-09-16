package com.edumind.course.dto.overview;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

@Data
public class CourseInstructorProfileItemDTO implements Serializable {
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @Size(max = 1000)
    private String intro;

    @Size(max = 200)
    private String officeHours;

    private Integer sortOrder;
    private Boolean primary;
}
