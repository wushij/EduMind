package com.edumind.teaching.api;

import com.edumind.teaching.vo.submission.SubmissionStatsVO;

import java.time.LocalDate;
import java.util.List;

/**
 * 作业提交只读查询 API（供 statistics 模块跨模块调用）
 */
public interface SubmissionQueryApi {

    SubmissionStatsVO getCourseSubmissionStats(Long courseId);

    List<SubmissionStatsVO.StudentScoreVO> listStudentScoresByCourse(Long courseId);

    /**
     * 按提交日期聚合真实均分（仅统计已批改答卷，百分制归一）。
     *
     * @param courseId  课程 ID；为 null 表示聚合全平台提交，作为"全校对照"基准线
     * @param startDate 起始日期（含），可为 null
     * @param endDate   结束日期（含），可为 null
     * @return 按日期升序的均分序列，无数据的日期不会出现
     */
    List<SubmissionStatsVO.DailyScoreVO> listDailyAvgScores(Long courseId, LocalDate startDate, LocalDate endDate);
}
