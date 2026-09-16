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
public class CourseCapabilityTagVO implements Serializable {
    private String code;
    private String label;
    private String tone;
}
