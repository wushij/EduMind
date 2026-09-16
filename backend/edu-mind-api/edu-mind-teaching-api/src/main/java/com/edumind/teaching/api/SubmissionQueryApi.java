package com.edumind.teaching.api;

import com.edumind.teaching.vo.submission.SubmissionStatsVO;

import java.util.List;

/**
 * 作业提交只读查询 API（供 statistics 模块跨模块调用）
 */
public interface SubmissionQueryApi {

    SubmissionStatsVO getCourseSubmissionStats(Long courseId);

    List<SubmissionStatsVO.StudentScoreVO> listStudentScoresByCourse(Long courseId);
}
