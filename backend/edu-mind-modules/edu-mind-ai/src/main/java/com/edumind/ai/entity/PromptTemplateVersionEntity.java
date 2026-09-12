package com.edumind.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("prompt_template_version")
public class PromptTemplateVersionEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long templateId;
    private Integer version;
    private String content;
    private String variables;
    private Long publishedBy;
    private LocalDateTime createTime;
}
