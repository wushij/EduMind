package com.edumind.course.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("course_learning_objective")
public class CourseLearningObjectiveEntity implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private Long courseId;
    private Integer sortOrder;
    private String title;
    private String description;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
