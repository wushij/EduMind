package com.edumind.statistics.service.teaching.support;

import com.edumind.statistics.entity.WrongQuestionRecordEntity;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;

/**
 * 同一考点错题分组的计数口径。
 *
 * <p>{@code wrong_question_record} 的一行是「一个学生 × 一道题」，因此分组后
 * <b>题目数、学生数、答错人次</b>三个口径完全不同，混用会直接产出一句错话：
 * 同一道题被 4 名学生答错时，"记录条数 = 4" 曾被当作题目数，界面写出
 * 「合并 4 道错题」，教师点开却只有 1 道题。</p>
 *
 * <p>三者都按去重口径计算，供薄弱考点榜单与「查看原题」抽屉共用。</p>
 */
public final class WrongQuestionGroupMetrics {

    private WrongQuestionGroupMetrics() {
    }

    /** 去重后的题目数：同一道题被多人答错仍只算 1 道 */
    public static int distinctQuestionCount(List<WrongQuestionRecordEntity> group) {
        if (group == null || group.isEmpty()) {
            return 0;
        }
        return (int) group.stream()
                .filter(Objects::nonNull)
                .map(WrongQuestionRecordEntity::getQuestionId)
                .filter(Objects::nonNull)
                .distinct()
                .count();
    }

    /** 去重后的答错学生数 */
    public static int distinctStudentCount(List<WrongQuestionRecordEntity> group) {
        if (group == null || group.isEmpty()) {
            return 0;
        }
        return (int) group.stream()
                .filter(Objects::nonNull)
                .map(WrongQuestionRecordEntity::getStudentId)
                .filter(Objects::nonNull)
                .distinct()
                .count();
    }

    /**
     * 分组内是否存在可归因的作答痕迹（学生确实提交过答案）。
     *
     * <p>空白作答的记录也会占一行错题，但没有任何作答内容可供归因：让这类考点按 0% 排在榜首，
     * 会把真正讲得清的薄弱点挤出前几名，教师点进去看到的也只是「系统不作认知归因」。
     * 榜单据此把「全为空白作答」的考点后置并单独标注。</p>
     */
    public static boolean hasAnsweringEvidence(List<WrongQuestionRecordEntity> group) {
        if (group == null || group.isEmpty()) {
            return false;
        }
        return group.stream()
                .filter(Objects::nonNull)
                .anyMatch(record -> StringUtils.hasText(record.getLastStudentAnswer()));
    }

    /** 累计答错人次（{@code wrong_count} 缺省按 1 次计） */
    public static int sumWrongCount(List<WrongQuestionRecordEntity> group) {
        if (group == null || group.isEmpty()) {
            return 0;
        }
        return group.stream()
                .filter(Objects::nonNull)
                .mapToInt(record -> record.getWrongCount() != null ? record.getWrongCount() : 1)
                .sum();
    }
}
