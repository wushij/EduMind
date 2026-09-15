package com.edumind.question.service.query.impl;

import com.edumind.common.exception.BusinessException;
import com.edumind.question.converter.QuestionConverter;
import com.edumind.question.dao.QuestionDao;
import com.edumind.question.entity.QuestionEntity;
import com.edumind.question.service.query.QuestionQueryService;
import com.edumind.question.vo.question.QuestionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuestionQueryServiceImpl implements QuestionQueryService {

    private final QuestionDao questionDao;
    private final QuestionConverter questionConverter;

    @Override
    public QuestionVO getQuestionById(Long questionId) {
        QuestionEntity entity = questionDao.getById(questionId);
        if (entity == null) {
            throw new BusinessException("题目不存在");
        }
        return questionConverter.toVO(entity);
    }

    @Override
    public List<QuestionVO> listQuestionsByIds(List<Long> questionIds) {
        if (questionIds == null || questionIds.isEmpty()) {
            return Collections.emptyList();
        }
        return questionDao.listByIds(questionIds).stream()
                .map(questionConverter::toVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<QuestionVO> listQuestionsByCourseId(Long courseId) {
        return questionDao.listByCourseId(courseId).stream()
                .map(questionConverter::toVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<QuestionVO> listQuestionsByCourseAndTypes(Long courseId, List<String> types, Integer limit) {
        return questionDao.listByCourseAndTypes(courseId, types, limit).stream()
                .map(questionConverter::toVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<QuestionVO> listQuestionsByKnowledgePointId(Long knowledgePointId, Integer limit) {
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
