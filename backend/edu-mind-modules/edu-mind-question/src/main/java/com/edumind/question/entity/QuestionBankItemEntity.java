package com.edumind.question.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("question_bank_item")
public class QuestionBankItemEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long bankId;
    private Long questionId;
    private LocalDateTime createTime;
}
