package com.edumind.teaching.service.query;

import com.edumind.teaching.vo.submission.SubmissionStatsVO;

import java.time.LocalDate;
import java.util.List;

public interface SubmissionStatsQueryService {

    SubmissionStatsVO getCourseSubmissionStats(Long courseId);

    List<SubmissionStatsVO.StudentScoreVO> listStudentScoresByCourse(Long courseId);

    List<SubmissionStatsVO.DailyScoreVO> listDailyAvgScores(Long courseId, LocalDate startDate, LocalDate endDate);
}
