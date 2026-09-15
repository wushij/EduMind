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
    private Long chapterCount;
    private Long knowledgePointCount;
    private Long resourceCount;
    private String category;
    private java.math.BigDecimal credits;
    private Integer plannedHours;
    private Long knowledgeBaseId;
    private String aiPersona;
    private String welcomeMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
