package com.edumind.teaching.api.impl;

import com.edumind.teaching.api.ExamQueryApi;
import com.edumind.teaching.service.assignment.AssignmentService;
import com.edumind.teaching.service.exam.ExamService;
import com.edumind.teaching.service.query.TeachingStatsQueryService;
import com.edumind.teaching.vo.assignment.AssignmentVO;
import com.edumind.teaching.vo.exam.ExamVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ExamQueryApiImpl implements ExamQueryApi {

    private final ExamService examService;
    private final AssignmentService assignmentService;
    private final TeachingStatsQueryService teachingStatsQueryService;

    @Override
    public ExamVO getExamById(Long examId) {
        return examService.getById(examId);
    }

    @Override
    public List<ExamVO> listExamsByCourseId(Long courseId) {
        return teachingStatsQueryService.listExamsByCourseId(courseId);
    }

    @Override
    public AssignmentVO getHomeworkById(Long homeworkId) {
        return assignmentService.getById(homeworkId);
    }

    @Override
    public long countExams() {
        return teachingStatsQueryService.countExams();
    }

    @Override
    public long countAssignments() {
        return teachingStatsQueryService.countAssignments();
    }

    @Override
    public long countPendingGrading() {
        return teachingStatsQueryService.countPendingGrading();
    }

    @Override
    public long countPendingAssignments() {
        return teachingStatsQueryService.countPendingAssignments();
    }
}
