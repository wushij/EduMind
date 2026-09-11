package com.edumind.question.dto.bank;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class QuestionBankAddQuestionsDTO {
    @NotEmpty(message = "题目ID列表不能为空")
    private List<Long> questionIds;
}
