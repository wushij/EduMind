package com.edumind.statistics.api;

import java.util.List;
import java.util.Map;

/**
 * 学习推荐只读查询 API（实现在 statistics 模块）
 */
public interface RecommendationQueryApi {

    List<Map<String, Object>> recommendResources(Long courseId, Long chapterId, Integer limit);
}
