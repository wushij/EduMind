package com.edumind.ai.dto.memory;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MemoryItemCreateDTO {
    private Long courseId;
    @NotBlank(message = "记忆摘要不能为空")
    private String summary;
    private String sensitivityLevel;
}
