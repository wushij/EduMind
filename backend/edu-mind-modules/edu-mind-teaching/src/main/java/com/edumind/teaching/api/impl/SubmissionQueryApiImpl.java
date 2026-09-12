package com.edumind.teaching.api.impl;

import com.edumind.teaching.api.SubmissionQueryApi;
import com.edumind.teaching.dao.AssignmentDao;
import com.edumind.teaching.dao.SubmissionDao;
import com.edumind.teaching.entity.AssignmentEntity;
import com.edumind.teaching.entity.SubmissionEntity;
import com.edumind.teaching.vo.submission.SubmissionStatsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SubmissionQueryApiImpl implements SubmissionQueryApi {

    private final AssignmentDao assignmentDao;
    private final SubmissionDao submissionDao;

    @Override
    public SubmissionStatsVO getCourseSubmissionStats(Long courseId) {
        SubmissionStatsVO stats = new SubmissionStatsVO();
        stats.setCourseId(courseId);
        List<AssignmentEntity> assignments = assignmentDao.listByCourseId(courseId);
        stats.setTotalAssignments(assignments.size());
        int submitted = 0;
        int graded = 0;
        double scoreSum = 0;
        int scoreCount = 0;
        for (AssignmentEntity assignment : assignments) {
            List<SubmissionEntity> subs = submissionDao.listByAssignmentId(assignment.getId());
            for (SubmissionEntity sub : subs) {
                if ("SUBMITTED".equals(sub.getStatus()) || "GRADED".equals(sub.getStatus())) {
                    submitted++;
                }
                if ("GRADED".equals(sub.getStatus()) && sub.getTotalScore() != null && sub.getMaxScore() != null
                        && sub.getMaxScore() > 0) {
                    graded++;
                    scoreSum += sub.getTotalScore() * 100.0 / sub.getMaxScore();
                    scoreCount++;
                }
            }
        }
        stats.setSubmittedCount(submitted);
        stats.setGradedCount(graded);
        int expected = assignments.size() > 0 ? assignments.size() * 2 : 0;
        stats.setAvgSubmissionRate(expected > 0 ? submitted * 100.0 / expected : 0);
        stats.setAvgScore(scoreCount > 0 ? scoreSum / scoreCount : 0);
        return stats;
    }

    @Override
    public List<SubmissionStatsVO.StudentScoreVO> listStudentScoresByCourse(Long courseId) {
        Map<Long, SubmissionStatsVO.StudentScoreVO> map = new HashMap<>();
        List<AssignmentEntity> assignments = assignmentDao.listByCourseId(courseId);
        for (AssignmentEntity assignment : assignments) {
            for (SubmissionEntity sub : submissionDao.listByAssignmentId(assignment.getId())) {
                SubmissionStatsVO.StudentScoreVO vo = map.computeIfAbsent(sub.getStudentId(), id -> {
                    SubmissionStatsVO.StudentScoreVO s = new SubmissionStatsVO.StudentScoreVO();
                    s.setStudentId(id);
                    s.setSubmissionCount(0);
                    s.setAvgScore(0.0);
                    return s;
                });
                vo.setSubmissionCount(vo.getSubmissionCount() + 1);
                if (sub.getTotalScore() != null && sub.getMaxScore() != null && sub.getMaxScore() > 0) {
                    double ratio = sub.getTotalScore() * 100.0 / sub.getMaxScore();
                    vo.setAvgScore((vo.getAvgScore() + ratio) / 2);
                }
            }
        }
        return new ArrayList<>(map.values());
    }
}
