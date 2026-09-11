package com.edumind.teaching.api.impl;

import com.edumind.teaching.api.ExamQueryApi;
import com.edumind.teaching.dao.AssignmentDao;
import com.edumind.teaching.dao.ExamDao;
import com.edumind.teaching.dao.SubmissionDao;
import com.edumind.teaching.entity.ExamEntity;
import com.edumind.teaching.service.exam.ExamService;
import com.edumind.teaching.vo.exam.ExamVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ExamQueryApiImpl implements ExamQueryApi {

    private final ExamDao examDao;
    private final AssignmentDao assignmentDao;
    private final SubmissionDao submissionDao;
    private final ExamService examService;

    @Override
    public Object getExamById(Long examId) {
        return examService.getById(examId);
    }

    @Override
    public List<?> listExamsByCourseId(Long courseId) {
        return examDao.findByCourseId(courseId).stream()
                .map(ExamEntity::getId)
                .map(examService::getById)
                .map(ExamVO.class::cast)
                .toList();
    }

    @Override
    public Object getHomeworkById(Long homeworkId) {
        return assignmentDao.findById(homeworkId);
    }

    @Override
    public long countExams() {
        return examDao.countAll();
    }

    @Override
    public long countAssignments() {
        return assignmentDao.countAll();
    }

    @Override
    public long countPendingGrading() {
        return submissionDao.countByStatus("SUBMITTED");
    }

    @Override
    public long countPendingAssignments() {
        return assignmentDao.countByStatus("PUBLISHED");
    }
}
