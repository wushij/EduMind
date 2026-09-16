package com.edumind.course.dto.overview;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

@Data
public class CourseObjectiveItemDTO implements Serializable {
    @NotBlank(message = "目标标题不能为空")
    @Size(max = 80, message = "目标标题不能超过80字")
    private String title;

    @Size(max = 500, message = "目标说明不能超过500字")
    private String description;
}
