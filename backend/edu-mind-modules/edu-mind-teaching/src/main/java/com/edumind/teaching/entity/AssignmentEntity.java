package com.edumind.teaching.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("assignment")
public class AssignmentEntity implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long courseId;
    private Long examId;
    private String title;
    private String description;
    private LocalDateTime deadline;
    private Integer totalScore;
    private Integer passScore;
    private String settingsJson;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
