package com.edumind.statistics.service.learning;

import com.edumind.statistics.vo.analytics.KnowledgeMasteryVO;
import com.edumind.statistics.vo.learning.RecommendedQuestionVO;
import com.edumind.statistics.vo.learning.RecommendedResourceVO;

import java.util.List;

public interface RecommendationService {

    List<RecommendedQuestionVO> recommendQuestions(Long courseId, Long chapterId, Integer limit);

    List<RecommendedQuestionVO> recommendQuestionsForStudent(
            Long courseId, Long chapterId, Integer limit, Long studentId);

    /**
     * 按学员薄弱考点推荐题目，并复用调用方已加载的掌握度。
     *
     * <p>掌握度计算需要跑「全班 × 全考点」矩阵，成本较高。学习路径编排等场景
     * 在逐周取题时若不复用，会对同一 (courseId, studentId) 反复重算，
     * 因此这里允许调用方把已算好的 {@code presetMastery} 传进来。</p>
     *
     * @param presetMastery 调用方已加载的掌握度；为 null 时由本方法自行加载
     */
    List<RecommendedQuestionVO> recommendQuestionsForStudent(
            Long courseId, Long chapterId, Integer limit, Long studentId, KnowledgeMasteryVO presetMastery);

    List<RecommendedResourceVO> recommendResources(Long courseId, Long chapterId, Integer limit);
}
