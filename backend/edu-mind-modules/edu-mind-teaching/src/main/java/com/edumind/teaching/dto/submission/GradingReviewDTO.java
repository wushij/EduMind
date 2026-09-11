package com.edumind.teaching.dto.submission;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class GradingReviewDTO implements Serializable {

    @NotEmpty(message = "复核项不能为空")
    @Valid
    private List<GradingReviewItemDTO> items;
}
