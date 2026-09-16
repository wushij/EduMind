package com.edumind.course.dto.overview;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class CourseObjectivesSaveDTO implements Serializable {
    @Valid
    @Size(max = 6, message = "教学目标最多6条")
    private List<CourseObjectiveItemDTO> objectives;
}
