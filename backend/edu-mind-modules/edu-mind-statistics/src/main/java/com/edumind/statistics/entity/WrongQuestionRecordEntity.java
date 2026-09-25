package com.edumind.statistics.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

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

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }

    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }

    public Long getQuestionId() { return questionId; }
    public void setQuestionId(Long questionId) { this.questionId = questionId; }

    public Long getKnowledgePointId() { return knowledgePointId; }
    public void setKnowledgePointId(Long knowledgePointId) { this.knowledgePointId = knowledgePointId; }

    public String getErrorTypes() { return errorTypes; }
    public void setErrorTypes(String errorTypes) { this.errorTypes = errorTypes; }

    public String getDiagnosis() { return diagnosis; }
    public void setDiagnosis(String diagnosis) { this.diagnosis = diagnosis; }

    public String getVariantQuestionIds() { return variantQuestionIds; }
    public void setVariantQuestionIds(String variantQuestionIds) { this.variantQuestionIds = variantQuestionIds; }

    public Integer getWrongCount() { return wrongCount; }
    public void setWrongCount(Integer wrongCount) { this.wrongCount = wrongCount; }

    public String getLastStudentAnswer() { return lastStudentAnswer; }
    public void setLastStudentAnswer(String lastStudentAnswer) { this.lastStudentAnswer = lastStudentAnswer; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public LocalDateTime getMasteredTime() { return masteredTime; }
    public void setMasteredTime(LocalDateTime masteredTime) { this.masteredTime = masteredTime; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
