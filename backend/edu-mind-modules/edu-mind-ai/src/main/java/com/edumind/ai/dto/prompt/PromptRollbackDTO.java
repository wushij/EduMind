package com.edumind.ai.dto.prompt;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PromptRollbackDTO {

    @NotNull(message = "目标版本号不能为空")
    private Integer targetVersion;
}
