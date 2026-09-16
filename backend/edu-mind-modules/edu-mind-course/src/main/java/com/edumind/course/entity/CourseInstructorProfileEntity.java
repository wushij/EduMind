package com.edumind.course.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("course_instructor_profile")
public class CourseInstructorProfileEntity implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private Long courseId;
    private Long userId;
    private String intro;
    private String officeHours;
    private Integer sortOrder;
    private Boolean isPrimary;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
