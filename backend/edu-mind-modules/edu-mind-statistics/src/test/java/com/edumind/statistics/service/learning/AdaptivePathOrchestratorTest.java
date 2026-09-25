package com.edumind.statistics.service.learning;

import com.edumind.course.api.CourseQueryApi;
import com.edumind.course.vo.chapter.ChapterTreeVO;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
        when(recommendationService.recommendQuestionsForStudent(eq(courseId), eq(10L), anyInt(), eq(studentId), any()))
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

    /**
     * 课程尚未录入知识点元数据（kpById 为空）时的降级行为：
     * 必须按章节维度给出互不相同的周主题与任务文案，且同一份计划内不重复推同一道练习题。
     */
    @Test
    void buildDetail_degradesToChapterPlan_whenKnowledgePointsMissing() {
        Long courseId = 103L;
        Long studentId = 1L;

        when(courseQueryApi.isCourseMember(courseId, studentId)).thenReturn(true);
        when(courseQueryApi.listCoursesByIds(List.of(courseId)))
                .thenReturn(List.of(com.edumind.course.vo.course.CourseVO.builder()
                        .id(courseId).name("高等数学（上）").build()));

        KnowledgeMasteryVO mastery = new KnowledgeMasteryVO();
        mastery.setWeakPoints(List.of());
        when(knowledgeMasteryService.getMastery(courseId, studentId)).thenReturn(mastery);
        when(knowledgeMasteryQueryApi.getMasteryByStudentAndCourse(studentId, courseId)).thenReturn(Map.of());
        when(courseQueryApi.listKnowledgePointsByCourseId(courseId)).thenReturn(List.of());
        when(courseQueryApi.listChaptersByCourseId(courseId)).thenReturn(List.of(
                ChapterTreeVO.builder().id(11L).parentId(0L).sort(1).title("第一章 函数与极限论")
                        .children(List.of(ChapterTreeVO.builder().id(12L).parentId(11L).sort(1)
                                .title("1.1 数列与函数极限计算").build()))
                        .build(),
                ChapterTreeVO.builder().id(13L).parentId(0L).sort(2).title("第二章 导数与微分").build()));

        RecommendedQuestionVO q1 = new RecommendedQuestionVO();
        q1.setId(1001L);
        q1.setStem("极限练习一");
        q1.setKnowledgePointId(17L);
        RecommendedQuestionVO q2 = new RecommendedQuestionVO();
        q2.setId(1002L);
        q2.setStem("极限练习二");
        q2.setKnowledgePointId(18L);
        when(recommendationService.recommendQuestionsForStudent(eq(courseId), any(), anyInt(), eq(studentId), any()))
                .thenReturn(List.of(q1, q2));

        LearningPathDetailVO detail = orchestrator.buildDetail(courseId, studentId);

        assertEquals(2, detail.getWeeks().size());
        assertEquals("第一章 函数与极限论", detail.getWeeks().get(0).getTheme());
        assertEquals("第二章 导数与微分", detail.getWeeks().get(1).getTheme());

        LearningPathVO.LearningPathTaskVO read1 = detail.getWeeks().get(0).getTasks().get(0);
        LearningPathVO.LearningPathTaskVO read2 = detail.getWeeks().get(1).getTasks().get(0);
        assertEquals("学习章节：第一章 函数与极限论", read1.getTitle());
        assertEquals("学习章节：第二章 导数与微分", read2.getTitle());
        assertNotEquals(read1.getTitle(), read2.getTitle());

        Long practiceId1 = detail.getWeeks().get(0).getTasks().get(1).getRefId();
        Long practiceId2 = detail.getWeeks().get(1).getTasks().get(1).getRefId();
        assertNotNull(practiceId1);
        assertNotEquals(practiceId1, practiceId2, "同一份学习计划内不应重复推荐同一道练习题");
    }

    /**
     * 掌握度复用守护：调用方传入 presetMastery 时，编排器不得再次加载掌握度。
     *
     * <p>掌握度计算需要跑「全班 × 全考点」矩阵，而学习路径会逐周取题；
     * 一旦这里退化为各自加载，学情画像这类接口会把它放大成十几次重复计算，
     * 直接表现为「选择学员后加载很久」。</p>
     */
    @Test
    void buildDetail_reusesPresetMastery_withoutReloading() {
        Long courseId = 103L;
        Long studentId = 1L;

        when(courseQueryApi.isCourseMember(courseId, studentId)).thenReturn(true);
        when(courseQueryApi.listCoursesByIds(List.of(courseId)))
                .thenReturn(List.of(com.edumind.course.vo.course.CourseVO.builder()
                        .id(courseId).name("高等数学（上）").build()));

        KnowledgeMasteryVO preset = new KnowledgeMasteryVO();
        preset.setWeakPoints(List.of());
        when(knowledgeMasteryQueryApi.getMasteryByStudentAndCourse(studentId, courseId)).thenReturn(Map.of());
        when(courseQueryApi.listKnowledgePointsByCourseId(courseId)).thenReturn(List.of());
        when(courseQueryApi.listChaptersByCourseId(courseId)).thenReturn(List.of(
                ChapterTreeVO.builder().id(11L).parentId(0L).sort(1).title("第一章 函数与极限论").build()));

        RecommendedQuestionVO q = new RecommendedQuestionVO();
        q.setId(2001L);
        q.setStem("章节练习");
        when(recommendationService.recommendQuestionsForStudent(eq(courseId), any(), anyInt(), eq(studentId), any()))
                .thenReturn(List.of(q));

        orchestrator.buildDetail(courseId, studentId, false, preset);

        verify(knowledgeMasteryService, never()).getMastery(courseId, studentId);
    }
}
