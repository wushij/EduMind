package com.edumind.statistics.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("wrong_question_record")
public class WrongQuestionRecordEntity implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long studentId;
    private Long courseId;
    private Long questionId;
    private Long knowledgePointId;
    private String errorTypes;
    private String diagnosis;
    private String variantQuestionIds;
    private Integer wrongCount;
    /** 最近一次错误作答 */
    private String lastStudentAnswer;
    /** 0=待攻坚 1=已攻克 */
    private Integer status;
    private LocalDateTime masteredTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
