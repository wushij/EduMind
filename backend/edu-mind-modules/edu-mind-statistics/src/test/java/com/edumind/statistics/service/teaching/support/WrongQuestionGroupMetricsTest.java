package com.edumind.statistics.service.teaching.support;

import com.edumind.statistics.entity.WrongQuestionRecordEntity;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 考点错题分组计数口径单测。
 *
 * <p>锁的是这次修掉的口径 bug：同一道题被多名学生答错时，
 * 「题目数」必须为 1，而不是记录条数 —— 否则界面会写出
 * 「合并 4 道错题」但抽屉里只有一道题。</p>
 */
class WrongQuestionGroupMetricsTest {

    private static WrongQuestionRecordEntity record(Long studentId, Long questionId, Integer wrongCount) {
        WrongQuestionRecordEntity entity = new WrongQuestionRecordEntity();
        entity.setStudentId(studentId);
        entity.setQuestionId(questionId);
        entity.setWrongCount(wrongCount);
        return entity;
    }

    private static WrongQuestionRecordEntity withAnswer(WrongQuestionRecordEntity entity, String answer) {
        entity.setLastStudentAnswer(answer);
        return entity;
    }

    @Test
    void 同一道题被四名学生答错时题目数为1学生数为4() {
        List<WrongQuestionRecordEntity> group = List.of(
                record(1L, 1102L, 1),
                record(2L, 1102L, 1),
                record(3L, 1102L, 1),
                record(4L, 1102L, 1));

        assertEquals(1, WrongQuestionGroupMetrics.distinctQuestionCount(group), "题目数不能等于记录条数");
        assertEquals(4, WrongQuestionGroupMetrics.distinctStudentCount(group));
        assertEquals(4, WrongQuestionGroupMetrics.sumWrongCount(group));
    }

    @Test
    void 多道题分组时按题目去重且人次累加() {
        List<WrongQuestionRecordEntity> group = List.of(
                record(1L, 1102L, 2),
                record(2L, 1102L, 1),
                record(1L, 1103L, 3));

        assertEquals(2, WrongQuestionGroupMetrics.distinctQuestionCount(group));
        assertEquals(2, WrongQuestionGroupMetrics.distinctStudentCount(group));
        assertEquals(6, WrongQuestionGroupMetrics.sumWrongCount(group));
    }

    @Test
    void 缺少题目或学生标识时不参与去重计数() {
        List<WrongQuestionRecordEntity> group = new ArrayList<>();
        group.add(record(1L, null, 1));
        group.add(record(null, 1102L, 1));
        group.add(null);

        assertEquals(1, WrongQuestionGroupMetrics.distinctQuestionCount(group));
        assertEquals(1, WrongQuestionGroupMetrics.distinctStudentCount(group));
        assertEquals(2, WrongQuestionGroupMetrics.sumWrongCount(group));
    }

    @Test
    void 空分组返回零且不抛异常() {
        assertEquals(0, WrongQuestionGroupMetrics.distinctQuestionCount(null));
        assertEquals(0, WrongQuestionGroupMetrics.distinctStudentCount(List.of()));
        assertEquals(0, WrongQuestionGroupMetrics.sumWrongCount(null));
    }

    @Test
    void 组内只要有学生提交过答案就算存在归因依据() {
        assertTrue(WrongQuestionGroupMetrics.hasAnsweringEvidence(List.of(
                record(1L, 1102L, 1),
                withAnswer(record(2L, 1102L, 1), "B"))));
    }

    @Test
    void 全为空白作答时没有可归因的作答痕迹() {
        assertFalse(WrongQuestionGroupMetrics.hasAnsweringEvidence(List.of(
                record(1L, 1102L, 1),
                record(2L, 1102L, 1))));
        assertFalse(WrongQuestionGroupMetrics.hasAnsweringEvidence(null));
        assertFalse(WrongQuestionGroupMetrics.hasAnsweringEvidence(List.of()));
    }
}
