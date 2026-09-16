package com.edumind.course.dto.overview;

import jakarta.validation.Valid;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class CourseInstructorsSaveDTO implements Serializable {
    @Valid
    private List<CourseInstructorProfileItemDTO> instructors;
}
