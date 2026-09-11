package com.edumind.resource.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("course_resource")
public class CourseResourceEntity implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long courseId;
    private Long resourceId;
    private Long documentId;
    private String title;
    private String resourceType;
    private LocalDateTime createTime;
}
