package com.edumind.statistics.api;

import java.util.List;
import java.util.Map;

/**
 * 掌握度只读查询 API（实现在 statistics 模块）
 */
public interface KnowledgeMasteryQueryApi {

    Map<Long, Double> getMasteryByStudentAndCourse(Long studentId, Long courseId);

    Map<String, Object> getStudentProfile(Long studentId, Long courseId);

    Map<Long, Double> getStudentsAverageMastery(List<Long> studentIds);

    Double getClassAverageMastery(List<Long> studentIds);

    Map<String, Object> getStudentOverallProfile(Long studentId);
}
