package com.edumind.question.vo.bank;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

import com.edumind.question.vo.question.QuestionVO;

@Data
public class QuestionBankVO {
    private Long id;
    private String name;
    private Long courseId;
    private String description;
    private Integer questionCount;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private List<QuestionVO> questions;
}
