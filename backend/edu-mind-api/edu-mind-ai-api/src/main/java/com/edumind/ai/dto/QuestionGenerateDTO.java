package com.edumind.ai.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class QuestionGenerateDTO {
    @NotNull(message = "课程ID不能为空")
    private Long courseId;
    private List<Long> chapterIds;
    private List<Long> knowledgePointIds;
    private List<String> knowledgePointNames;
    private List<String> questionTypes;
    private String difficulty;
    @NotNull(message = "出题数量不能为空")
    private Integer count;
    private Integer scorePerQuestion;
    /**
     * 教师专属命题指令 / 教学提示词要求
     */
    private String promptDirective;
    /**
     * 命题应用场景（随堂测验、阶段考核、难点突破、错题变式等）
     */
    private String questionScene;
    /**
     * 关联知识库文档 ID 列表（用于 RAG 增强）
     */
    private List<Long> documentIds;
}
