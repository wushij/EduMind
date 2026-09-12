package com.edumind.ai.vo.prompt;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PromptTemplateVersionVO {
    private Long id;
    private Long templateId;
    private Integer version;
    private String content;
    private String variables;
    private Long publishedBy;
    private LocalDateTime createTime;
}
