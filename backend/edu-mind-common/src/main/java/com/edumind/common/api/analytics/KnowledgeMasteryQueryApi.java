package com.edumind.common.api.analytics;

import java.util.Map;

/**
 * 掌握度只读查询 API（接口在 common，实现在 statistics）
 */
public interface KnowledgeMasteryQueryApi {

    Map<Long, Double> getMasteryByStudentAndCourse(Long studentId, Long courseId);
}
