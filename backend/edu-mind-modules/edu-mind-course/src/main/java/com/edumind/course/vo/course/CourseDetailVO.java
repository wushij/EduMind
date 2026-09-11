package com.edumind.course.vo.course;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseDetailVO implements Serializable {

    private Long id;
    private String name;
    private String code;
    private String description;
    private String coverUrl;
    private String semester;
    private String status;
    private Long teacherId;
    private String teacherName;
    private Long studentCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
