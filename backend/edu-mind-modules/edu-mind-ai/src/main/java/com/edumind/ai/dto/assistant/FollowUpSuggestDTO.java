package com.edumind.ai.dto.assistant;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 追问建议生成入参。
 *
 * <p>追问必须由「本轮真实问答」推演而来：写入 {@code question} / {@code answer} 的文本
 * 就是模型唯一的素材，模型只被允许围绕其中的具体术语与结论提问，
 * 从而避免出现「还有什么想了解的」这类与本轮内容无关的通用模板提问。</p>
 */
@Data
public class FollowUpSuggestDTO {

    @NotBlank(message = "本轮用户提问不能为空")
    private String question;

    @NotBlank(message = "本轮助手回答不能为空")
    private String answer;

    /** 可选场景上下文（课程名 / 课节标题 / 题目考点），用于让追问贴合当前入口；为空不影响生成 */
    private String context;

    /** 生成条数，默认 3，上限 5 */
    private Integer count;

    /** 前端选择的模型配置键；为空时走场景策略与平台默认模型 */
    private String modelKey;
}
