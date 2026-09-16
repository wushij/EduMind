package com.edumind.ai.dto.memory;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MemoryItemUpdateDTO {
    @NotBlank(message = "记忆摘要不可为空")
    private String summary;
    private String memoryType;
    private String sensitivityLevel;
    private String fullContent;
}
