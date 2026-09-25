package com.edumind.ai.dto.memory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MemoryItemCreateDTO {
    private Long courseId;
    /** 上限须与 MemorySummaryNormalizer.MAX_SUMMARY_CHARS 保持一致（此处写字面量以避免 dto 反向依赖 service） */
    @NotBlank(message = "记忆摘要不能为空")
    @Size(max = 1000, message = "记忆摘要不得超过 1000 字，请精简后提交")
    private String summary;
    private String fullContent;
    private String memoryType;
    private String sensitivityLevel;
}
