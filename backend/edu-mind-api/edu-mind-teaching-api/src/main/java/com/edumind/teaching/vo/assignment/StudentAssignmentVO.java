package com.edumind.teaching.vo.assignment;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class StudentAssignmentVO extends AssignmentVO {
    /** 当前学生提交状态：NOT_STARTED / IN_PROGRESS / SUBMITTED / GRADED */
    private String mySubmissionStatus;
    private Long mySubmissionId;
}
