package com.edumind.question.vo.question;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class QuestionVO {
    /**
     * edu_question.id 为 19 位雪花 ID，已超出 JavaScript 的 Number.MAX_SAFE_INTEGER(2^53-1)。
     * 若按 JSON 数字下发，浏览器 JSON.parse 会静默丢精度，前端回传的 questionId 就匹配不到
     * 库中真实题目（试卷保存唯一键冲突、试卷详情题干空白均源于此），故统一序列化为字符串。
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private Long bankId;
    private Long courseId;
    private Long knowledgePointId;
    private String stem;
    private String type;
    private String options;
    private String answer;
    private String analysis;
    private Integer difficulty;
    private Integer score;
    private Integer status;
    private String knowledgePointName;
    private String cognitiveLevel;
    private String distractorAnalysis;
    private LocalDateTime createTime;
}
