package com.edumind.teaching.dto.submission;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class SubmissionCreateDTO {
    @NotEmpty(message = "作答列表不能为空")
    private List<SubmissionAnswerItemDTO> answers;
}
