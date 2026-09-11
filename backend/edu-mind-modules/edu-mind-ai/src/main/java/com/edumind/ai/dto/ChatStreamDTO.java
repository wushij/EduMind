package com.edumind.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChatStreamDTO {
    private String conversationId;
    private Long courseId;

    @NotBlank(message = "消息内容不能为空")
    private String message;
}
