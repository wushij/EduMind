package com.edumind.teaching.api;

import java.util.List;

/**
 * 教学领域（考试/作业）跨模块只读查询公开 API
 * 供 Statistics（学情分析）、AI 智能批改模块跨域调用
 */
public interface ExamQueryApi {

    Object getExamById(Long examId);

    List<?> listExamsByCourseId(Long courseId);

    Object getHomeworkById(Long homeworkId);

    long countExams();

    long countAssignments();

    long countPendingGrading();

    long countPendingAssignments();
}
