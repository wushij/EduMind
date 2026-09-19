package com.edumind.course.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("course_knowledge_point")
public class KnowledgePointEntity implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long courseId;
    private Long chapterId;
    private String title;
    private String code;
    private String description;
    private String cognitiveDimension;
    private Integer importance;
    private String examFocus;
    private Integer sortOrder;
    private LocalDateTime createTime;
}
