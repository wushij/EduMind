package com.edumind.teaching.service.submission;

import com.edumind.common.api.PageResult;
import com.edumind.teaching.dto.submission.SubmissionBatchGradeDTO;
import com.edumind.teaching.vo.submission.SubmissionListItemVO;
import com.edumind.teaching.vo.submission.SubmissionOverviewStatsVO;

public interface SubmissionOverviewService {

    PageResult<SubmissionListItemVO> pageQuery(Long courseId, Long assignmentId, String status, String keyword,
                                               Long page, Long pageSize);

    SubmissionOverviewStatsVO getStats(Long courseId, Long assignmentId);

    int batchGrade(SubmissionBatchGradeDTO dto);
}
