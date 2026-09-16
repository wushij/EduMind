package com.edumind.course.vo.overview;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseObjectiveVO implements Serializable {
    private Long id;
    private Integer sortOrder;
    private String title;
    private String description;
}
