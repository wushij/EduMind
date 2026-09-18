package com.edumind.course.vo.course;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseVO implements Serializable {

    private Long id;
    private String name;
    private String coverUrl;
    private String teacherName;
    /** 主讲教师头像 URL（来自用户档案） */
    private String teacherAvatar;
    private Long teacherId;
    private Long studentCount;
    private String status;
    private String semester;
    private String code;
    private String description;
    private Long chapterCount;
    private Long knowledgePointCount;
    private Long resourceCount;
    private String category;
    private java.math.BigDecimal credits;
    private Integer plannedHours;
    private Long knowledgeBaseId;
    private String aiPersona;
    private String welcomeMessage;
}
