package com.edumind.teaching.vo.exam;

import com.edumind.question.vo.question.QuestionVO;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

@Data
public class ExamQuestionVO {
    /** 与 QuestionVO.id 同理：雪花 ID 序列化为字符串，保证「换一题」回传的 oldQuestionId 精确 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long questionId;
    private Integer score;
    private Integer sortOrder;
    private QuestionVO question;
}
