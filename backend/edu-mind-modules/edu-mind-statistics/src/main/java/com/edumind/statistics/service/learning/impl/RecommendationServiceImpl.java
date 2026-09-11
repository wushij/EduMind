package com.edumind.statistics.service.learning.impl;

import com.edumind.question.api.QuestionQueryApi;
import com.edumind.question.vo.question.QuestionVO;
import com.edumind.resource.api.ResourceQueryApi;
import com.edumind.resource.vo.ResourceVO;
import com.edumind.statistics.service.learning.RecommendationService;
import com.edumind.statistics.vo.learning.RecommendedQuestionVO;
import com.edumind.statistics.vo.learning.RecommendedResourceVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecommendationServiceImpl implements RecommendationService {

    private final QuestionQueryApi questionQueryApi;
    private final ResourceQueryApi resourceQueryApi;

    @Override
    public List<RecommendedQuestionVO> recommendQuestions(Long courseId, Long chapterId, Integer limit) {
        if (courseId == null) {
            return Collections.emptyList();
        }
        List<?> questions = questionQueryApi.listQuestionsByCourseId(courseId);
        List<QuestionVO> pool = questions.stream()
                .map(item -> (QuestionVO) item)
                .collect(Collectors.toCollection(ArrayList::new));
        Collections.shuffle(pool);
        int size = limit != null && limit > 0 ? limit : 10;
        return pool.stream()
                .limit(size)
                .map(this::toQuestionRecommendation)
                .collect(Collectors.toList());
    }

    @Override
    public List<RecommendedResourceVO> recommendResources(Long courseId, Long chapterId, Integer limit) {
        if (courseId == null) {
            return Collections.emptyList();
        }
        int size = limit != null && limit > 0 ? limit : 10;
        List<?> resources = resourceQueryApi.listResourcesByCourse(courseId, chapterId, size * 3);
        List<ResourceVO> pool = resources.stream()
                .map(item -> (ResourceVO) item)
                .collect(Collectors.toCollection(ArrayList::new));
        Collections.shuffle(pool);
        return pool.stream()
                .limit(size)
                .map(this::toResourceRecommendation)
                .collect(Collectors.toList());
    }

    private RecommendedQuestionVO toQuestionRecommendation(QuestionVO question) {
        RecommendedQuestionVO vo = new RecommendedQuestionVO();
        vo.setId(question.getId());
        vo.setStem(question.getStem());
        vo.setType(question.getType());
        vo.setDifficulty(question.getDifficulty());
        vo.setCourseId(question.getCourseId());
        return vo;
    }

    private RecommendedResourceVO toResourceRecommendation(ResourceVO resource) {
        RecommendedResourceVO vo = new RecommendedResourceVO();
        vo.setId(resource.getId());
        vo.setTitle(resource.getTitle());
        vo.setResourceType(resource.getResourceType());
        vo.setFileUrl(resource.getFileUrl());
        vo.setCourseId(resource.getCourseId());
        vo.setChapterId(resource.getChapterId());
        return vo;
    }
}
