package com.edumind.teaching.api.impl;

import com.edumind.teaching.api.SubmissionQueryApi;
import com.edumind.teaching.service.query.SubmissionStatsQueryService;
import com.edumind.teaching.vo.submission.SubmissionStatsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SubmissionQueryApiImpl implements SubmissionQueryApi {

    private final SubmissionStatsQueryService submissionStatsQueryService;

    @Override
    public SubmissionStatsVO getCourseSubmissionStats(Long courseId) {
        return submissionStatsQueryService.getCourseSubmissionStats(courseId);
    }

    @Override
    public List<SubmissionStatsVO.StudentScoreVO> listStudentScoresByCourse(Long courseId) {
        return submissionStatsQueryService.listStudentScoresByCourse(courseId);
    }

    @Override
    public List<SubmissionStatsVO.DailyScoreVO> listDailyAvgScores(Long courseId, LocalDate startDate, LocalDate endDate) {
        return submissionStatsQueryService.listDailyAvgScores(courseId, startDate, endDate);
    }
}
