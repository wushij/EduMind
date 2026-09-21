package com.edumind.statistics.service.learning;

import com.edumind.course.api.CourseQueryApi;
import com.edumind.statistics.dao.LearningRecordDao;
import com.edumind.statistics.service.analytics.LearningAnalyticsService;
import com.edumind.statistics.service.learning.impl.LearningReportServiceImpl;
import com.edumind.statistics.vo.analytics.StudentPortraitVO;
import com.edumind.statistics.vo.learning.LearningReportVO;
import com.edumind.teaching.api.SubmissionQueryApi;
import com.edumind.teaching.vo.submission.SubmissionStatsVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LearningReportServiceImplTest {

    @Mock
    private LearningAnalyticsService learningAnalyticsService;
    @Mock
    private CourseQueryApi courseQueryApi;
    @Mock
    private LearningRecordDao learningRecordDao;
    @Mock
    private SubmissionQueryApi submissionQueryApi;

    @InjectMocks
    private LearningReportServiceImpl learningReportService;

    @Test
    void getReport_resolvesCourseAndPortrait() {
        when(courseQueryApi.listCourseIdsByUserId(3L)).thenReturn(List.of(102L));
        when(courseQueryApi.isCourseMember(102L, 3L)).thenReturn(true);
        when(courseQueryApi.listCoursesByIds(List.of(102L))).thenReturn(List.of());

        StudentPortraitVO portrait = new StudentPortraitVO();
        portrait.getSummary().setTotalStudyMinutes(45);
        portrait.getSummary().setAiUsageCount(2);
        when(learningAnalyticsService.getStudentPortrait(102L, 3L, "30d")).thenReturn(portrait);

        when(learningRecordDao.listByCourseAndStudentSince(eq(102L), eq(3L), any())).thenReturn(List.of());
        when(submissionQueryApi.getCourseSubmissionStats(102L)).thenReturn(new SubmissionStatsVO());

        LearningReportVO report = learningReportService.getReport(3L, null, "30d");

        assertNotNull(report);
        assertEquals(102L, report.getCourseId());
        assertNotNull(report.getPortrait());
        assertEquals(45, report.getPortrait().getSummary().getTotalStudyMinutes());
    }
}
