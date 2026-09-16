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
public class CourseInstructorCardVO implements Serializable {
    private Long userId;
    private String username;
    private String realName;
    private String avatar;
    private String memberRole;
    private String roleLabel;
    private String intro;
    private String officeHours;
    private Boolean primary;
    private Integer sortOrder;
}
