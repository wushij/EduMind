package com.edumind.statistics.service.learning;

import com.edumind.statistics.vo.learning.LearningReportVO;

public interface LearningReportService {

    LearningReportVO getReport(Long studentId, Long courseId, String range);
}
