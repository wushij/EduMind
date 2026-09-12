package com.edumind.question.api.impl;

import com.edumind.common.exception.BusinessException;
import com.edumind.question.api.QuestionQueryApi;
import com.edumind.question.converter.QuestionConverter;
import com.edumind.question.dao.QuestionDao;
import com.edumind.question.entity.QuestionEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuestionQueryApiImpl implements QuestionQueryApi {

    private final QuestionDao questionDao;
    private final QuestionConverter questionConverter;

    @Override
    public Object getQuestionById(Long questionId) {
        QuestionEntity entity = questionDao.getById(questionId);
        if (entity == null) {
            throw new BusinessException("题目不存在");
        }
        return questionConverter.toVO(entity);
    }

    @Override
    public List<?> listQuestionsByIds(List<Long> questionIds) {
        if (questionIds == null || questionIds.isEmpty()) {
            return Collections.emptyList();
        }
        return questionDao.listByIds(questionIds).stream()
                .map(questionConverter::toVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<?> listQuestionsByCourseId(Long courseId) {
        return questionDao.listByCourseId(courseId).stream()
                .map(questionConverter::toVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<?> listQuestionsByCourseAndTypes(Long courseId, List<String> types, Integer limit) {
        return questionDao.listByCourseAndTypes(courseId, types, limit).stream()
                .map(questionConverter::toVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<?> listQuestionsByKnowledgePointId(Long knowledgePointId, Integer limit) {
        if (knowledgePointId == null) {
            return Collections.emptyList();
        }
        return questionDao.listByKnowledgePointId(knowledgePointId, limit).stream()
                .map(questionConverter::toVO)
                .collect(Collectors.toList());
    }

    @Override
    public long countQuestions() {
        return questionDao.countAll();
    }
}
