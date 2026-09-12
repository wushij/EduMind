package com.edumind.statistics.service.teaching;

import com.edumind.statistics.vo.teaching.TeachingReportVO;

public interface TeachingReportService {

    TeachingReportVO buildReport(Long courseId, String range);
}
