package com.edumind.ai.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ConversationVO {
    private String id;
    private Long userId;
    private Long courseId;
    private String title;
    private Integer messageCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
