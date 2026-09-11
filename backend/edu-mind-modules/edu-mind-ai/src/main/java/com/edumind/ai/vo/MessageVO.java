package com.edumind.ai.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MessageVO {
    private String id;
    private String conversationId;
    private String role;
    private String content;
    private LocalDateTime createTime;
}
