package com.edumind.question.vo.question;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionBatchSaveVO {
    private Integer savedCount;
    private List<Long> questionIds;
}
