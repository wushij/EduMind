package com.edumind.question.service.question;

import com.edumind.common.api.PageResult;
import com.edumind.question.dto.question.QuestionBatchCreateDTO;
import com.edumind.question.dto.question.QuestionCreateDTO;
import com.edumind.question.dto.question.QuestionQueryDTO;
import com.edumind.question.dto.question.QuestionUpdateDTO;
import com.edumind.question.vo.question.QuestionBatchSaveVO;
import com.edumind.question.vo.question.QuestionVO;

import java.util.List;

public interface QuestionService {
    Long createQuestion(QuestionCreateDTO dto);

    QuestionBatchSaveVO batchSave(QuestionBatchCreateDTO dto);

    QuestionVO getQuestionById(Long id);

    PageResult<QuestionVO> pageQuery(QuestionQueryDTO query);

    void updateQuestion(Long id, QuestionUpdateDTO dto);

    void deleteQuestion(Long id);

    List<QuestionVO> listQuestionsByIds(List<Long> ids);

    List<QuestionVO> listQuestionsByKnowledgePointId(Long pointId, Integer limit);

    List<QuestionVO> listQuestionsByCourseId(Long courseId);

    List<QuestionVO> listQuestionsByCourseAndTypes(Long courseId, List<String> types, Integer limit);
}
