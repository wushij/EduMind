package com.edumind.teaching.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("grading_result")
public class GradingResultEntity implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long submissionId;
    private Long questionId;
    private Integer score;
    private Integer maxScore;
    private Integer isCorrect;
    private String aiComment;
    private String teacherComment;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
