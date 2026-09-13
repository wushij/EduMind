package com.edumind.ai.dto.memory;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MemoryConsentDTO {
    private Long courseId;
    @NotNull(message = "授权状态不能为空")
    private Boolean consent;
}
