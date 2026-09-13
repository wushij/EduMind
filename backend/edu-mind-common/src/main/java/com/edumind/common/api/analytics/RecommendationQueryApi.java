package com.edumind.common.api.analytics;

import java.util.List;
import java.util.Map;

/**
 * 学习推荐只读查询 API（接口在 common，实现在 statistics）
 */
public interface RecommendationQueryApi {

    List<Map<String, Object>> recommendResources(Long courseId, Long chapterId, Integer limit);
}
