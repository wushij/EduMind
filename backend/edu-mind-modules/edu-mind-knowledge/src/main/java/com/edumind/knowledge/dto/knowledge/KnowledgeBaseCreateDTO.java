package com.edumind.knowledge.dto.knowledge;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class KnowledgeBaseCreateDTO {
    @NotBlank(message = "知识库名称不能为空")
    private String name;
    private String description;
    private Long courseId;
}
