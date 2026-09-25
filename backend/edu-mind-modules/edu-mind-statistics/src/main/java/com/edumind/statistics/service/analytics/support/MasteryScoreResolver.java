package com.edumind.statistics.service.analytics.support;

import com.edumind.course.vo.knowledge.KnowledgePointVO;
import com.edumind.statistics.entity.KnowledgeMasteryEntity;
import com.edumind.statistics.entity.WrongQuestionRecordEntity;
import com.edumind.teaching.vo.submission.SubmissionStatsVO;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 掌握度分值解析器 —— 全模块唯一的「原始学情数据 → 掌握度分值」换算入口。
 *
 * <p>背景：{@code KnowledgeMasteryServiceImpl} 的雷达图接口与热力矩阵接口原先各自复制了一份
 * 「作业均分为基准 + 错题扣减 + 考点重要度微调」的推算代码。任何一处调整都会造成同一个页面上
 * 出现两套数字（例如 Hero 班级均分 42.9% 与矩阵列均分 52%/24%/24% 无法互相验证）。</p>
 *
 * <p>本解析器保证两件事：</p>
 * <ol>
 *   <li><b>唯一真相</b>：同一 (学生, 考点) 组合在任何接口、任何调用路径下得到的分值完全一致；</li>
 *   <li><b>来源可追溯</b>：每个分值都携带 {@link MasterySource} 标记，前端可以区分「实测成绩」与
 *       「规则推算」，不再把先验基准 {@value #PRIOR_BASE_RATIO} 伪装成真实测评结果。</li>
 * </ol>
 *
 * <p>分值语义统一为 0.0 ~ 1.0 的比例值（前端 ×100 得到百分比）。</p>
 */
@Component
public class MasteryScoreResolver {

    /** 学员在该课程无任何作业成绩时使用的先验良性基准（对应界面上凭空出现的 72%） */
    public static final double PRIOR_BASE_RATIO = 0.72;

    /** 存在错题失分时的掌握度下限（允许压得更低以体现认知漏洞） */
    private static final double MIN_PENALTY_SCORE = 0.20;
    /** 无错题时基准微调路径的下限（无失分证据不应被压得过低） */
    private static final double MIN_ADJUSTED_SCORE = 0.40;
    /** 推算结果上限 */
    private static final double MAX_SCORE = 0.95;

    /** 单次错题造成的掌握度扣减步长 */
    private static final double WRONG_PENALTY_STEP = 0.15;

    /** 考点重要度每偏离基准档位一档的微调系数 */
    private static final double IMPORTANCE_ADJUST_STEP = 0.03;

    /** 考点重要度基准档位（3 档为中性，不做微调） */
    private static final int DEFAULT_IMPORTANCE = 3;

    /** 作业均分换算为掌握度基准时的边界，避免 0 分或满分造成极端基准 */
    private static final double SUBMISSION_RATIO_FLOOR = 0.30;
    private static final double SUBMISSION_RATIO_CEIL = 0.98;

    /**
     * 掌握度数据来源。
     */
    public enum MasterySource {
        /** 来自 {@code knowledge_mastery} 表的真实测评记录（由自适应练习/测评写回） */
        MEASURED,
        /** 由作业均分、错题数与考点重要度推算得出，并非真实作答结果 */
        ESTIMATED
    }

    /**
     * 单个 (学生, 考点) 的解析结果。
     *
     * @param score       掌握度分值，0.0 ~ 1.0
     * @param source      数据来源
     * @param sampleCount 实测样本数（推算结果为 0）
     */
    public record ResolvedScore(double score, MasterySource source, int sampleCount) {

        public boolean isMeasured() {
            return source == MasterySource.MEASURED;
        }
    }

    /**
     * 一次统计所需的全部原始数据索引。构建一次即可服务该课程下的全部 (学生, 考点) 组合，
     * 避免在双层循环里反复查找。
     */
    public static final class MasteryContext {

        private final Map<Long, Double> submissionRatioMap;
        private final Map<String, KnowledgeMasteryEntity> measuredMap;
        private final Map<String, Integer> wrongCountMap;
        private final Map<Long, Integer> kpWrongTotalMap;

        private MasteryContext(Map<Long, Double> submissionRatioMap,
                               Map<String, KnowledgeMasteryEntity> measuredMap,
                               Map<String, Integer> wrongCountMap,
                               Map<Long, Integer> kpWrongTotalMap) {
            this.submissionRatioMap = submissionRatioMap;
            this.measuredMap = measuredMap;
            this.wrongCountMap = wrongCountMap;
            this.kpWrongTotalMap = kpWrongTotalMap;
        }

        /**
         * 解析某学生在某考点上的掌握度。
         *
         * <p>优先级：实测记录 &gt; 错题扣减推算 &gt; 作业基准 + 重要度微调推算。</p>
         */
        public ResolvedScore resolve(Long studentId, KnowledgePointVO point) {
            if (studentId == null || point == null || point.getId() == null) {
                return new ResolvedScore(PRIOR_BASE_RATIO, MasterySource.ESTIMATED, 0);
            }

            KnowledgeMasteryEntity measured = measuredMap.get(key(studentId, point.getId()));
            if (measured != null) {
                double score = clamp(measured.getMasteryScore().doubleValue());
                int samples = measured.getSampleCount() != null && measured.getSampleCount() > 0
                        ? measured.getSampleCount()
                        : 1;
                return new ResolvedScore(score, MasterySource.MEASURED, samples);
            }

            double baseRatio = submissionRatioMap.getOrDefault(studentId, PRIOR_BASE_RATIO);
            int wrongs = wrongCountMap.getOrDefault(key(studentId, point.getId()), 0);
            double score;
            if (wrongs > 0) {
                score = Math.max(MIN_PENALTY_SCORE, baseRatio - wrongs * WRONG_PENALTY_STEP);
            } else {
                int importance = point.getImportance() != null ? point.getImportance() : DEFAULT_IMPORTANCE;
                double adjust = (DEFAULT_IMPORTANCE - importance) * IMPORTANCE_ADJUST_STEP;
                score = Math.max(MIN_ADJUSTED_SCORE, Math.min(MAX_SCORE, baseRatio + adjust));
            }
            return new ResolvedScore(score, MasterySource.ESTIMATED, 0);
        }

        /** 某考点在全班的错题累计次数（真实统计，不再按比例反推） */
        public int wrongCountOfPoint(Long knowledgePointId) {
            return kpWrongTotalMap.getOrDefault(knowledgePointId, 0);
        }

        /** 该 (学生, 考点) 是否已有实测记录 */
        public boolean hasMeasured(Long studentId, Long knowledgePointId) {
            return measuredMap.containsKey(key(studentId, knowledgePointId));
        }

        private static String key(Long studentId, Long knowledgePointId) {
            return studentId + "_" + knowledgePointId;
        }

        private static double clamp(double value) {
            return Math.max(0.0, Math.min(1.0, value));
        }
    }

    /**
     * 构建统计上下文。三个入参允许为 null，代表对应数据源暂无数据。
     *
     * @param submissionStats 课程作业提交与成绩统计
     * @param masteries       课程下全部实测掌握度记录
     * @param wrongRecords    课程下全部错题记录
     */
    public MasteryContext buildContext(SubmissionStatsVO submissionStats,
                                       List<KnowledgeMasteryEntity> masteries,
                                       List<WrongQuestionRecordEntity> wrongRecords) {
        Map<Long, Double> submissionRatioMap = new HashMap<>();
        if (submissionStats != null && submissionStats.getStudentScores() != null) {
            for (SubmissionStatsVO.StudentScoreVO score : submissionStats.getStudentScores()) {
                if (score == null || score.getStudentId() == null || score.getAvgScore() == null) {
                    continue;
                }
                double ratio = Math.max(SUBMISSION_RATIO_FLOOR,
                        Math.min(SUBMISSION_RATIO_CEIL, score.getAvgScore() / 100.0));
                submissionRatioMap.putIfAbsent(score.getStudentId(), ratio);
            }
        }

        Map<String, KnowledgeMasteryEntity> measuredMap = new HashMap<>();
        if (masteries != null) {
            for (KnowledgeMasteryEntity entity : masteries) {
                if (entity == null || entity.getStudentId() == null
                        || entity.getKnowledgePointId() == null || entity.getMasteryScore() == null) {
                    continue;
                }
                measuredMap.put(entity.getStudentId() + "_" + entity.getKnowledgePointId(), entity);
            }
        }

        Map<String, Integer> wrongCountMap = new HashMap<>();
        Map<Long, Integer> kpWrongTotalMap = new HashMap<>();
        if (wrongRecords != null) {
            for (WrongQuestionRecordEntity record : wrongRecords) {
                if (record == null || record.getStudentId() == null || record.getKnowledgePointId() == null) {
                    continue;
                }
                int count = record.getWrongCount() != null ? record.getWrongCount() : 1;
                // 同一学生对同一考点可能存在多条错题记录（不同题目），必须累加而非覆盖
                wrongCountMap.merge(record.getStudentId() + "_" + record.getKnowledgePointId(), count, Integer::sum);
                kpWrongTotalMap.merge(record.getKnowledgePointId(), count, Integer::sum);
            }
        }

        return new MasteryContext(submissionRatioMap, measuredMap, wrongCountMap, kpWrongTotalMap);
    }

    /**
     * 空上下文，用于课程无考点或数据源异常时的安全降级。
     */
    public MasteryContext emptyContext() {
        return buildContext(null, Collections.emptyList(), Collections.emptyList());
    }
}
