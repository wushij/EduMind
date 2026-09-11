package com.edumind.statistics.service.learning;

import com.edumind.statistics.vo.learning.RecommendedQuestionVO;
import com.edumind.statistics.vo.learning.RecommendedResourceVO;

import java.util.List;

public interface RecommendationService {

    List<RecommendedQuestionVO> recommendQuestions(Long courseId, Long chapterId, Integer limit);

    List<RecommendedResourceVO> recommendResources(Long courseId, Long chapterId, Integer limit);
}
