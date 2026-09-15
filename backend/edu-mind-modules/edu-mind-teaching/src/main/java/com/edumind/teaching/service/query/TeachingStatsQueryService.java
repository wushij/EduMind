package com.edumind.teaching.service.query;

import com.edumind.teaching.vo.exam.ExamVO;

import java.util.List;

public interface TeachingStatsQueryService {

    List<ExamVO> listExamsByCourseId(Long courseId);

    long countExams();

    long countAssignments();

    long countPendingGrading();

    long countPendingAssignments();
}
