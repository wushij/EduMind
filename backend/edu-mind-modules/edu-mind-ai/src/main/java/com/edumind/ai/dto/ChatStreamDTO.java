package com.edumind.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class ChatStreamDTO {
    private String conversationId;
    private Long courseId;

    @NotBlank(message = "消息内容不能为空")
    private String message;

    private Long knowledgeBaseId;
    private Boolean useRag;
    private Long chapterId;
    private Long lessonChapterId;
    /** 当前知识锚定章节/微课节标题（如「1.1 数列与函数极限计算」） */
    private String sectionTitle;
    private Long documentId;

    /** 临时上传的附件ID列表（用于文档解析与上下文理解） */
    private List<String> attachmentIds;

    /** 是否启用联网搜索增强 */
    private Boolean webSearch;

    /** 前端选择的模型配置标识（config_name / model_key），为空时使用默认对话模型 */
    private String modelKey;

    /** 是否为重新生成：为 true 时不重复保存用户消息，并替换上一轮 assistant 回复 */
    private Boolean regenerate;
}
