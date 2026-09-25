package com.edumind.statistics.service.analytics.impl;

import com.edumind.course.api.CourseQueryApi;
import com.edumind.course.vo.knowledge.KnowledgePointVO;
import com.edumind.statistics.dao.KnowledgeMasteryDao;
import com.edumind.statistics.dao.WrongQuestionRecordDao;
import com.edumind.statistics.entity.KnowledgeMasteryEntity;
import com.edumind.statistics.entity.WrongQuestionRecordEntity;
import com.edumind.statistics.service.analytics.support.MasteryScoreResolver;
import com.edumind.statistics.vo.analytics.KnowledgeMasteryVO;
import com.edumind.system.api.OrganizationQueryApi;
import com.edumind.system.api.UserQueryApi;
import com.edumind.system.vo.user.UserBriefVO;
import com.edumind.teaching.api.SubmissionQueryApi;
import com.edumind.teaching.vo.submission.SubmissionStatsVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

/**
 * 课程掌握度画像服务测试。
 *
 * <p>场景取自课程 103《高等数学（上）》的真实数据形态：</p>
 * <ul>
 *   <li>选课成员：张老师(TEACHER)、杨同学(3)、王同学(4) —— 学员名单只应保留后两人；</li>
 *   <li>{@code knowledge_mastery} / {@code wrong_question_record} 中还存在管理员(1) 的孤儿记录；</li>
 *   <li>王同学没有任何作业、错题与实测记录，只能靠先验基准推算。</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class KnowledgeMasteryServiceImplTest {

    private static final Long COURSE_ID = 103L;

    @Mock
    private KnowledgeMasteryDao knowledgeMasteryDao;
    @Mock
    private WrongQuestionRecordDao wrongQuestionRecordDao;
    @Mock
    private CourseQueryApi courseQueryApi;
    @Mock
    private UserQueryApi userQueryApi;
    @Mock
    private OrganizationQueryApi organizationQueryApi;
    @Mock
    private SubmissionQueryApi submissionQueryApi;
    @Spy
    private MasteryScoreResolver masteryScoreResolver = new MasteryScoreResolver();

    @InjectMocks
    private KnowledgeMasteryServiceImpl service;

    private static KnowledgePointVO kp(long id, String title) {
        KnowledgePointVO vo = new KnowledgePointVO();
        vo.setId(id);
        vo.setTitle(title);
        vo.setChapterId(12L);
        vo.setImportance(3);
        return vo;
    }

    private static UserBriefVO user(long id, String username, String realName) {
        return UserBriefVO.builder().id(id).username(username).realName(realName).build();
    }

    private static KnowledgeMasteryEntity mastery(long studentId, long kpId, double score) {
        KnowledgeMasteryEntity entity = new KnowledgeMasteryEntity();
        entity.setStudentId(studentId);
        entity.setKnowledgePointId(kpId);
        entity.setMasteryScore(BigDecimal.valueOf(score));
        entity.setSampleCount(3);
        return entity;
    }

    private static WrongQuestionRecordEntity wrong(long studentId, long kpId, int count) {
        WrongQuestionRecordEntity entity = new WrongQuestionRecordEntity();
        entity.setStudentId(studentId);
        entity.setKnowledgePointId(kpId);
        entity.setWrongCount(count);
        return entity;
    }

    private static Map<Long, UserBriefVO> roster(List<Long> ids) {
        Map<Long, UserBriefVO> map = new HashMap<>();
        for (Long id : ids) {
            map.put(id, user(id, id == 3L ? "student" : "student2", id == 3L ? "杨同学" : "王同学"));
        }
        return map;
    }

    private static KnowledgeMasteryVO.WeakPointVO findWeak(KnowledgeMasteryVO vo, long kpId) {
        return vo.getWeakPoints().stream()
                .filter(w -> Long.valueOf(kpId).equals(w.getKnowledgePointId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("weak point not found: " + kpId));
    }

    @BeforeEach
    void setUp() {
        when(courseQueryApi.listStudentUserIdsByCourseId(COURSE_ID)).thenReturn(List.of(3L, 4L));
        when(courseQueryApi.listKnowledgePointsByCourseId(COURSE_ID)).thenReturn(List.of(
                kp(17, "等价无穷小代换及其应用条件"),
                kp(18, "洛必达法则求未定式极限"),
                kp(19, "复合函数链式求导法则")));
        when(courseQueryApi.listChaptersByCourseId(COURSE_ID)).thenReturn(Collections.emptyList());
        when(userQueryApi.mapUserBriefsByIds(anyList()))
                .thenAnswer(invocation -> roster(invocation.getArgument(0)));
        when(organizationQueryApi.mapPrimaryClassesByUserIds(any(), anyList()))
                .thenReturn(Collections.emptyMap());
        when(submissionQueryApi.getCourseSubmissionStats(COURSE_ID)).thenReturn(new SubmissionStatsVO());
    }

    @Test
    @DisplayName("非选课成员（管理员）的掌握度与错题不进入班级统计")
    void orphanRecordsAreExcluded() {
        when(knowledgeMasteryDao.listByCourse(COURSE_ID)).thenReturn(List.of(
                mastery(3L, 17L, 0.425),
                mastery(1L, 17L, 0.40),
                mastery(1L, 18L, 0.0)));
        when(wrongQuestionRecordDao.listByCourse(COURSE_ID)).thenReturn(List.of(
                wrong(3L, 18L, 1), wrong(3L, 18L, 1), wrong(3L, 18L, 1), wrong(3L, 18L, 1),
                wrong(1L, 18L, 2), wrong(1L, 18L, 2), wrong(1L, 18L, 2), wrong(1L, 18L, 2)));

        KnowledgeMasteryVO vo = service.getMastery(COURSE_ID, null);

        assertEquals(2, vo.getStudentCount().intValue());
        // KP17：杨同学实测 0.425、王同学推算 0.72 → 57.25 分
        assertEquals(57, vo.getClassAvg().get(0).intValue());
        // 失分累积只统计班级成员，应为 4 次，而不是含管理员的 12 次
        assertEquals(4, findWeak(vo, 18L).getWrongCount().intValue());
    }

    @Test
    @DisplayName("传入非选课成员 ID 时视为未聚焦，不会再凭空多出一名学员")
    void nonMemberStudentIdDoesNotBecomeFocus() {
        when(knowledgeMasteryDao.listByCourse(COURSE_ID)).thenReturn(List.of(mastery(3L, 17L, 0.425)));
        when(wrongQuestionRecordDao.listByCourse(COURSE_ID)).thenReturn(Collections.emptyList());

        // 教师(2) 打开全班视图时曾经的兜底路径
        KnowledgeMasteryVO vo = service.getMastery(COURSE_ID, 2L);

        assertEquals(KnowledgeMasteryVO.SCOPE_CLASS, vo.getScope());
        assertNull(vo.getFocusStudentId());
        assertTrue(vo.getPersonal().isEmpty());
        assertEquals(2, vo.getStudentCount().intValue());
    }

    @Test
    @DisplayName("聚焦真实学员时，指标计数与薄弱榜单严格同源")
    void focusStudentKeepsCountsConsistentWithWeakPoints() {
        when(knowledgeMasteryDao.listByCourse(COURSE_ID)).thenReturn(List.of(
                mastery(3L, 17L, 0.90), mastery(3L, 18L, 0.90), mastery(3L, 19L, 0.90)));
        when(wrongQuestionRecordDao.listByCourse(COURSE_ID)).thenReturn(Collections.emptyList());

        KnowledgeMasteryVO vo = service.getMastery(COURSE_ID, 3L);

        assertEquals(KnowledgeMasteryVO.SCOPE_STUDENT, vo.getScope());
        assertEquals(3L, vo.getFocusStudentId());
        assertEquals(3, vo.getPersonal().size());
        assertEquals(3, vo.getMasteredCount().intValue());
        assertEquals(0, vo.getWarningCount().intValue());
        // 曾经的缺陷：指标条按班级口径算、榜单按个人口径算，两者永远对不上
        assertTrue(vo.getWeakPoints().isEmpty());
    }

    @Test
    @DisplayName("includeTesting 开关只影响人数口径，且被过滤的账号不参与均分")
    void testingAccountsAreExcludedByDefault() {
        when(courseQueryApi.listStudentUserIdsByCourseId(COURSE_ID)).thenReturn(List.of(1L, 3L, 4L));
        when(userQueryApi.mapUserBriefsByIds(anyList())).thenAnswer(invocation -> {
            Map<Long, UserBriefVO> map = new HashMap<>();
            for (Long id : invocation.<List<Long>>getArgument(0)) {
                map.put(id, id == 1L
                        ? user(1L, "admin", "超级管理员")
                        : user(id, id == 3L ? "student" : "student2", id == 3L ? "杨同学" : "王同学"));
            }
            return map;
        });
        when(knowledgeMasteryDao.listByCourse(COURSE_ID)).thenReturn(Collections.emptyList());
        when(wrongQuestionRecordDao.listByCourse(COURSE_ID)).thenReturn(Collections.emptyList());

        assertEquals(2, service.getMastery(COURSE_ID, null, false).getStudentCount().intValue());
        assertEquals(3, service.getMastery(COURSE_ID, null, true).getStudentCount().intValue());
    }

    @Test
    @DisplayName("热力矩阵与画像使用同一批学员与同一份班级均分")
    void heatmapSharesRosterAndAverageWithProfile() {
        when(knowledgeMasteryDao.listByCourse(COURSE_ID)).thenReturn(List.of(mastery(3L, 17L, 0.425)));
        when(wrongQuestionRecordDao.listByCourse(COURSE_ID)).thenReturn(Collections.emptyList());

        KnowledgeMasteryVO vo = service.getMastery(COURSE_ID, null);
        Map<String, Object> heatmap = service.getHeatmap(COURSE_ID, "30d");

        assertEquals(vo.getStudentCount(), heatmap.get("studentCount"));
        @SuppressWarnings("unchecked")
        Map<String, Object> classAvgScores = (Map<String, Object>) heatmap.get("classAvgScores");
        // 矩阵列头均分必须能与指标条的班级均分互相验证
        assertEquals(57.0, (Double) classAvgScores.get("17"), 0.6);
        assertEquals(vo.getClassAvg().get(0).intValue(), (int) Math.round((Double) classAvgScores.get("17")));
    }
}
