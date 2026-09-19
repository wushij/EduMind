package com.edumind.statistics.service.learning;

import com.edumind.course.api.CourseQueryApi;
import com.edumind.course.vo.course.CourseDetailVO;
import com.edumind.course.vo.knowledge.KnowledgePointVO;
import com.edumind.knowledge.api.KnowledgeGraphQueryApi;
import com.edumind.knowledge.api.KnowledgePointRelationCommandApi;
import com.edumind.statistics.api.KnowledgeMasteryQueryApi;
import com.edumind.statistics.dao.WrongQuestionRecordDao;
import com.edumind.statistics.service.analytics.KnowledgeMasteryService;
import com.edumind.statistics.vo.analytics.KnowledgeMasteryVO;
import com.edumind.statistics.vo.learning.LearningPathDetailVO;
import com.edumind.statistics.vo.learning.LearningPathVO;
import com.edumind.statistics.vo.learning.RecommendedQuestionVO;
import com.edumind.system.api.UserQueryApi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdaptivePathOrchestratorTest {

    @Mock
    private KnowledgeMasteryService knowledgeMasteryService;
    @Mock
    private CourseQueryApi courseQueryApi;
    @Mock
    private RecommendationService recommendationService;
    @Mock
    private LearningPathService learningPathService;
    @Mock
    private KnowledgePointRelationCommandApi knowledgePointRelationCommandApi;
    @Mock
    private KnowledgeGraphQueryApi knowledgeGraphQueryApi;
    @Mock
    private WrongQuestionRecordDao wrongQuestionRecordDao;
    @Mock
    private UserQueryApi userQueryApi;
    @Mock
    private KnowledgeMasteryQueryApi knowledgeMasteryQueryApi;

    @InjectMocks
    private AdaptivePathOrchestrator orchestrator;

    @Test
    void buildDetail_includesTargetUrlOnTasks() {
        Long courseId = 101L;
        Long studentId = 2L;
        Long kpId = 501L;

        when(courseQueryApi.isCourseMember(courseId, studentId)).thenReturn(true);
        when(courseQueryApi.listCoursesByIds(List.of(courseId)))
                .thenReturn(List.of(com.edumind.course.vo.course.CourseVO.builder().id(courseId).name("Java").build()));

        KnowledgeMasteryVO.WeakPointVO weak = new KnowledgeMasteryVO.WeakPointVO();
        weak.setKnowledgePointId(kpId);
        weak.setTitle("包结构");
        weak.setMastery(0.3);
        weak.setSuggestion("需加强练习");

        KnowledgeMasteryVO mastery = new KnowledgeMasteryVO();
        mastery.setWeakPoints(List.of(weak));
        when(knowledgeMasteryService.getMastery(courseId, studentId)).thenReturn(mastery);
        when(knowledgeMasteryQueryApi.getMasteryByStudentAndCourse(studentId, courseId))
                .thenReturn(Map.of(kpId, 0.3));

        KnowledgePointVO kp = KnowledgePointVO.builder().id(kpId).title("包结构").chapterId(10L).courseId(courseId).build();
        when(courseQueryApi.listKnowledgePointsByCourseId(courseId)).thenReturn(List.of(kp));
        when(knowledgePointRelationCommandApi.listPrerequisiteTargetsBySourceIds(List.of(kpId))).thenReturn(Map.of());

        RecommendedQuestionVO q = new RecommendedQuestionVO();
        q.setId(9001L);
        q.setStem("测试题");
        q.setKnowledgePointId(kpId);
        when(recommendationService.recommendQuestionsForStudent(eq(courseId), eq(10L), anyInt(), eq(studentId)))
                .thenReturn(List.of(q));
        when(wrongQuestionRecordDao.findByStudentCourseAndKp(studentId, courseId, kpId)).thenReturn(null);

        CourseDetailVO detailCourse = new CourseDetailVO();
        detailCourse.setKnowledgeBaseId(null);
        when(courseQueryApi.getCourseById(courseId)).thenReturn(detailCourse);

        LearningPathDetailVO detail = orchestrator.buildDetail(courseId, studentId);

        assertNotNull(detail);
        assertFalse(detail.getWeeks().isEmpty());
        LearningPathVO.LearningPathTaskVO firstTask = detail.getWeeks().get(0).getTasks().get(0);
        assertNotNull(firstTask.getTargetUrl());
        assertTrue(firstTask.getTargetUrl().contains("/course/" + courseId));
    }
}
