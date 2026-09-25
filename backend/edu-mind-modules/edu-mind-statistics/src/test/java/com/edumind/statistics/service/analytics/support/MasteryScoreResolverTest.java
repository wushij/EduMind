package com.edumind.statistics.service.analytics.support;

import com.edumind.course.vo.knowledge.KnowledgePointVO;
import com.edumind.statistics.entity.KnowledgeMasteryEntity;
import com.edumind.statistics.entity.WrongQuestionRecordEntity;
import com.edumind.teaching.vo.submission.SubmissionStatsVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 掌握度分值解析器单元测试。
 *
 * <p>核心保护的是一条不变量：同一个 (学生, 考点) 组合，不论由雷达图接口还是热力矩阵接口调用，
 * 都必须得到完全相同的分值——这正是修复前 42.9% 与 52%/24%/24% 并存问题的根因。</p>
 */
class MasteryScoreResolverTest {

    private static final double EPS = 1e-9;

    private final MasteryScoreResolver resolver = new MasteryScoreResolver();

    private static KnowledgePointVO point(long id, Integer importance) {
        KnowledgePointVO vo = new KnowledgePointVO();
        vo.setId(id);
        vo.setImportance(importance);
        return vo;
    }

    private static KnowledgeMasteryEntity measured(long studentId, long kpId, double score, int samples) {
        KnowledgeMasteryEntity entity = new KnowledgeMasteryEntity();
        entity.setStudentId(studentId);
        entity.setKnowledgePointId(kpId);
        entity.setMasteryScore(BigDecimal.valueOf(score));
        entity.setSampleCount(samples);
        return entity;
    }

    private static WrongQuestionRecordEntity wrong(long studentId, long kpId, int wrongCount) {
        WrongQuestionRecordEntity entity = new WrongQuestionRecordEntity();
        entity.setStudentId(studentId);
        entity.setKnowledgePointId(kpId);
        entity.setWrongCount(wrongCount);
        return entity;
    }

    private static SubmissionStatsVO submissions(long studentId, double avgScore) {
        SubmissionStatsVO.StudentScoreVO score = new SubmissionStatsVO.StudentScoreVO();
        score.setStudentId(studentId);
        score.setAvgScore(avgScore);
        SubmissionStatsVO stats = new SubmissionStatsVO();
        stats.setStudentScores(List.of(score));
        return stats;
    }

    @Test
    @DisplayName("实测记录优先于一切推算")
    void measuredScoreWins() {
        var context = resolver.buildContext(
                submissions(1L, 90.0),
                List.of(measured(1L, 10L, 0.33, 5)),
                List.of(wrong(1L, 10L, 3)));

        var resolved = context.resolve(1L, point(10L, 3));

        assertTrue(resolved.isMeasured());
        assertEquals(MasteryScoreResolver.MasterySource.MEASURED, resolved.source());
        assertEquals(0.33, resolved.score(), EPS);
        assertEquals(5, resolved.sampleCount());
    }

    @Test
    @DisplayName("完全没有数据时返回先验基准，并明确标记为推算")
    void fallsBackToPriorBaseline() {
        var context = resolver.buildContext(null, Collections.emptyList(), Collections.emptyList());

        var resolved = context.resolve(7L, point(10L, 3));

        assertFalse(resolved.isMeasured());
        assertEquals(MasteryScoreResolver.PRIOR_BASE_RATIO, resolved.score(), EPS);
        assertEquals(0, resolved.sampleCount());
    }

    @Test
    @DisplayName("同一考点的多条错题记录按次数累加，不再相互覆盖")
    void wrongRecordsAreAccumulated() {
        var context = resolver.buildContext(
                null,
                Collections.emptyList(),
                List.of(wrong(1L, 10L, 2), wrong(1L, 10L, 1)));

        var resolved = context.resolve(1L, point(10L, 3));

        // 0.72 - 3 * 0.15 = 0.27
        assertEquals(0.27, resolved.score(), EPS);
        assertEquals(3, context.wrongCountOfPoint(10L));
    }

    @Test
    @DisplayName("错题扣减触底后不低于 0.20")
    void wrongPenaltyHasFloor() {
        var context = resolver.buildContext(
                null, Collections.emptyList(), List.of(wrong(1L, 10L, 9)));

        assertEquals(0.20, context.resolve(1L, point(10L, 3)).score(), EPS);
    }

    @Test
    @DisplayName("作业均分决定基准，考点重要度做微调")
    void submissionScoreDrivesBaseline() {
        var context = resolver.buildContext(
                submissions(1L, 80.0), Collections.emptyList(), Collections.emptyList());

        // 0.80 + (3 - 1) * 0.03 = 0.86
        assertEquals(0.86, context.resolve(1L, point(10L, 1)).score(), EPS);
        // 0.80 + (3 - 5) * 0.03 = 0.74
        assertEquals(0.74, context.resolve(1L, point(11L, 5)).score(), EPS);
    }

    @Test
    @DisplayName("作业均分被夹在 0.30 ~ 0.98，避免 0 分与满分造成极端基准")
    void submissionRatioIsClamped() {
        var low = resolver.buildContext(submissions(1L, 0.0), Collections.emptyList(), Collections.emptyList());
        var high = resolver.buildContext(submissions(2L, 100.0), Collections.emptyList(), Collections.emptyList());

        // 基准被夹到 0.30，再受无错题路径下限 0.40 约束
        assertEquals(0.40, low.resolve(1L, point(10L, 3)).score(), EPS);
        // 0.98 - 0.03 = 0.95
        assertEquals(0.95, high.resolve(2L, point(10L, 4)).score(), EPS);
    }

    @Test
    @DisplayName("同一份上下文对同一入参始终给出同一分值")
    void resolutionIsDeterministic() {
        var context = resolver.buildContext(
                submissions(1L, 66.0), List.of(measured(2L, 10L, 0.5, 2)), List.of(wrong(1L, 11L, 1)));

        double first = context.resolve(1L, point(10L, 3)).score();
        double second = context.resolve(1L, point(10L, 3)).score();

        assertEquals(first, second, EPS);
        assertEquals(0.66, first, EPS);
    }
}
