package com.edumind.question.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("edu_question")
public class QuestionEntity {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long bankId;
    private Long courseId;
    private Long knowledgePointId;
    private String stem;
    private String type; // SINGLE_CHOICE, MULTIPLE_CHOICE, JUDGMENT, BLANK, ESSAY
    private String options;
    private String answer;
    private String analysis;
    private Integer difficulty; // 1-5
    private Integer score;
    private Integer status;
    @TableLogic
    private Integer deleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
