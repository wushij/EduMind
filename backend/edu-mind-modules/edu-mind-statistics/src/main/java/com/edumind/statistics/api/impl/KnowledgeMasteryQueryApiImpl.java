package com.edumind.statistics.api.impl;

import com.edumind.common.api.analytics.KnowledgeMasteryQueryApi;
import com.edumind.statistics.service.query.KnowledgeMasteryQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class KnowledgeMasteryQueryApiImpl implements KnowledgeMasteryQueryApi {

    private final KnowledgeMasteryQueryService knowledgeMasteryQueryService;

    @Override
    public Map<Long, Double> getMasteryByStudentAndCourse(Long studentId, Long courseId) {
        return knowledgeMasteryQueryService.getMasteryByStudentAndCourse(studentId, courseId);
    }

    @Override
    public Map<String, Object> getStudentProfile(Long studentId, Long courseId) {
        return knowledgeMasteryQueryService.getStudentProfile(studentId, courseId);
    }

    @Override
    public Map<Long, Double> getStudentsAverageMastery(List<Long> studentIds) {
        return knowledgeMasteryQueryService.getStudentsAverageMastery(studentIds);
    }

    @Override
    public Double getClassAverageMastery(List<Long> studentIds) {
        return knowledgeMasteryQueryService.getClassAverageMastery(studentIds);
    }

    @Override
    public Map<String, Object> getStudentOverallProfile(Long studentId) {
        return knowledgeMasteryQueryService.getStudentOverallProfile(studentId);
    }
}
