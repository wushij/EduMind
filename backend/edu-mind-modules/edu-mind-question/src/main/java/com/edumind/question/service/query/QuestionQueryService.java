package com.edumind.question.service.query;

import com.edumind.question.vo.question.QuestionVO;

import java.util.List;

public interface QuestionQueryService {

    QuestionVO getQuestionById(Long questionId);

    List<QuestionVO> listQuestionsByIds(List<Long> questionIds);

    List<QuestionVO> listQuestionsByCourseId(Long courseId);

    List<QuestionVO> listQuestionsByCourseAndTypes(Long courseId, List<String> types, Integer limit);

    List<QuestionVO> listQuestionsByKnowledgePointId(Long knowledgePointId, Integer limit);

    long countQuestions();
}
