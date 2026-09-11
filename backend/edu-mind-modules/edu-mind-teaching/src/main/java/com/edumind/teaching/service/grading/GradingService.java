package com.edumind.teaching.service.grading;

import com.edumind.teaching.dto.submission.GradingReviewDTO;
import com.edumind.teaching.vo.submission.GradingItemVO;

import java.util.List;

public interface GradingService {

    void gradeSubmission(Long submissionId);

    List<GradingItemVO> getGradingResults(Long submissionId);

    void reviewGrading(Long submissionId, GradingReviewDTO dto);
}
