package com.edumind.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ConversationRenameDTO {
    @NotBlank(message = "会话标题不能为空")
    private String title;
}
