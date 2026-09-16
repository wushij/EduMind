package com.edumind.statistics.api.impl;

import com.edumind.statistics.api.RecommendationQueryApi;
import com.edumind.statistics.service.learning.RecommendationService;
import com.edumind.statistics.vo.learning.RecommendedResourceVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecommendationQueryApiImpl implements RecommendationQueryApi {

    private final RecommendationService recommendationService;

    @Override
    public List<Map<String, Object>> recommendResources(Long courseId, Long chapterId, Integer limit) {
        return recommendationService.recommendResources(courseId, chapterId, limit).stream()
                .map(this::toMap)
                .collect(Collectors.toList());
    }

    private Map<String, Object> toMap(RecommendedResourceVO vo) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", vo.getId());
        map.put("title", vo.getTitle());
        map.put("type", vo.getResourceType());
        map.put("url", vo.getFileUrl());
        map.put("courseId", vo.getCourseId());
        map.put("chapterId", vo.getChapterId());
        return map;
    }
}
