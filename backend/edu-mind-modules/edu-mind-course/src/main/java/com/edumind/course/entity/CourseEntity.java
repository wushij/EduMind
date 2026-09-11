package com.edumind.course.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 课程持久化实体
 */
@Data
@TableName("course")
public class CourseEntity implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String code;
    private Long teacherId;
    private String semester;
    private String description;
    private String coverImage;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
