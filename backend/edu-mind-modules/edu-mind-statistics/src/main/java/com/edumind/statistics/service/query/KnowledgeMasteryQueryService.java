package com.edumind.statistics.service.query;

import java.util.List;
import java.util.Map;

public interface KnowledgeMasteryQueryService {

    Map<Long, Double> getMasteryByStudentAndCourse(Long studentId, Long courseId);

    Map<String, Object> getStudentProfile(Long studentId, Long courseId);

    Map<Long, Double> getStudentsAverageMastery(List<Long> studentIds);

    Double getClassAverageMastery(List<Long> studentIds);

    Map<String, Object> getStudentOverallProfile(Long studentId);
}
