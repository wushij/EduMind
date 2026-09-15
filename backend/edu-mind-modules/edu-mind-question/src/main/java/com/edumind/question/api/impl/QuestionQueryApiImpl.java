package com.edumind.question.api.impl;

import com.edumind.question.api.QuestionQueryApi;
import com.edumind.question.service.query.QuestionQueryService;
import com.edumind.question.vo.question.QuestionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class QuestionQueryApiImpl implements QuestionQueryApi {

    private final QuestionQueryService questionQueryService;

    @Override
    public QuestionVO getQuestionById(Long questionId) {
        return questionQueryService.getQuestionById(questionId);
    }

    @Override
    public List<QuestionVO> listQuestionsByIds(List<Long> questionIds) {
        return questionQueryService.listQuestionsByIds(questionIds);
    }

    @Override
    public List<QuestionVO> listQuestionsByCourseId(Long courseId) {
        return questionQueryService.listQuestionsByCourseId(courseId);
    }

    @Override
    public List<QuestionVO> listQuestionsByCourseAndTypes(Long courseId, List<String> types, Integer limit) {
        return questionQueryService.listQuestionsByCourseAndTypes(courseId, types, limit);
    }

    @Override
    public List<QuestionVO> listQuestionsByKnowledgePointId(Long knowledgePointId, Integer limit) {
        return questionQueryService.listQuestionsByKnowledgePointId(knowledgePointId, limit);
    }

    @Override
    public long countQuestions() {
        return questionQueryService.countQuestions();
    }
}
