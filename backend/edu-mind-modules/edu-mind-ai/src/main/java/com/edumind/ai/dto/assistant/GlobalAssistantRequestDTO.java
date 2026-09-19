package com.edumind.ai.dto.assistant;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GlobalAssistantRequestDTO {

    @NotBlank(message = "用户提问内容不能为空")
    private String message;

    private Long courseId;

    private String conversationId;

    /** 课节章节 ID（微课节） */
    private Long lessonChapterId;

    /** lesson_studio / lesson_learn / course_space / question_bank / global */
    private String contextModule;

    private Long questionId;

    private String selectedText;

    private String draftExcerpt;

    private String draftTitle;

    private String draftDescription;

    private String objectiveExcerpt;

    /**
     * 兼容前端历史传参 input 别名
     */
    public void setInput(String input) {
        if (this.message == null || this.message.isBlank()) {
            this.message = input;
        }
    }
}
