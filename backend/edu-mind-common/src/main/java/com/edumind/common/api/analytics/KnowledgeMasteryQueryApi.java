package com.edumind.common.api.analytics;

import java.util.List;
import java.util.Map;

/**
 * 掌握度只读查询 API（接口在 common，实现在 statistics）
 */
public interface KnowledgeMasteryQueryApi {

    Map<Long, Double> getMasteryByStudentAndCourse(Long studentId, Long courseId);

    /**
     * 学生学情画像（掌握度、薄弱点等）
     */
    Map<String, Object> getStudentProfile(Long studentId, Long courseId);

    /**
     * 批量查询学生的平均知识掌握度 (0.00 ~ 1.00)
     */
    Map<Long, Double> getStudentsAverageMastery(List<Long> studentIds);

    /**
     * 获取指定学生群体的班级平均知识掌握度 (0.00 ~ 1.00)
     */
    Double getClassAverageMastery(List<Long> studentIds);

    /**
     * 获取学生全局学情认知画像（跨课程聚合）
     */
    Map<String, Object> getStudentOverallProfile(Long studentId);
}

