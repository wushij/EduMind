package com.edumind.statistics.service.learning;

import com.edumind.course.api.CourseQueryApi;
import com.edumind.statistics.dao.LearningRecordDao;
import com.edumind.statistics.service.analytics.KnowledgeMasteryService;
import com.edumind.statistics.service.learning.impl.LearningHomeServiceImpl;
import com.edumind.statistics.vo.learning.LearningHomeOverviewVO;
import com.edumind.teaching.api.StudentAssignmentQueryApi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LearningHomeServiceImplTest {

    @Mock
    private CourseQueryApi courseQueryApi;
    @Mock
    private KnowledgeMasteryService knowledgeMasteryService;
    @Mock
    private LearningRecordDao learningRecordDao;
    @Mock
    private StudentAssignmentQueryApi studentAssignmentQueryApi;
    @Mock
    private AdaptivePathService adaptivePathService;

    @InjectMocks
    private LearningHomeServiceImpl learningHomeService;

    @Test
    void getOverview_returnsEmptyWhenNoCourses() {
        when(courseQueryApi.listRecentCourses(20)).thenReturn(Collections.emptyList());
        when(courseQueryApi.listCourseIdsByUserId(1L)).thenReturn(Collections.emptyList());

        LearningHomeOverviewVO overview = learningHomeService.getOverview(1L, null);

        assertTrue(overview.getCourses().isEmpty());
        assertTrue(overview.getWeakPoints().isEmpty());
        assertTrue(overview.getTodayTasks().isEmpty());
    }

    @Test
    void getOverview_resolvesPrimaryCourseFromMembershipList() {
        when(courseQueryApi.listRecentCourses(20)).thenReturn(Collections.emptyList());
        when(courseQueryApi.listCourseIdsByUserId(2L)).thenReturn(List.of(101L));
        when(courseQueryApi.listCoursesByIds(List.of(101L))).thenReturn(Collections.emptyList());
        when(studentAssignmentQueryApi.listMine(null)).thenReturn(Collections.emptyList());
        when(courseQueryApi.getLessonProgressSummary(101L, 2L))
                .thenReturn(com.edumind.course.vo.lesson.CourseLessonProgressSummaryVO.builder()
                        .totalLessonCount(10)
                        .completedLessonCount(5)
                        .build());
        when(knowledgeMasteryService.getMastery(101L, 2L))
                .thenReturn(new com.edumind.statistics.vo.analytics.KnowledgeMasteryVO());
        when(adaptivePathService.buildAdaptivePath(101L, 2L))
                .thenReturn(new com.edumind.statistics.vo.learning.LearningPathVO());

        LearningHomeOverviewVO overview = learningHomeService.getOverview(2L, null);

        assertEquals(101L, overview.getPrimaryCourseId());
        assertEquals(50, overview.getSummary().getAvgCourseProgressPercent());
    }
}
