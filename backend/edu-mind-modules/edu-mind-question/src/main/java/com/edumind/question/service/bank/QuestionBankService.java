package com.edumind.question.service.bank;

import com.edumind.common.api.PageResult;
import com.edumind.question.dto.bank.QuestionBankAddQuestionsDTO;
import com.edumind.question.dto.bank.QuestionBankCreateDTO;
import com.edumind.question.dto.bank.QuestionBankUpdateDTO;
import com.edumind.question.vo.bank.QuestionBankVO;

public interface QuestionBankService {

    PageResult<QuestionBankVO> pageQuery(Long courseId, String keyword, Long page, Long pageSize);

    QuestionBankVO getById(Long id, boolean includeQuestions);

    Long create(QuestionBankCreateDTO dto);

    void update(Long id, QuestionBankUpdateDTO dto);

    void delete(Long id);

    void addQuestions(Long id, QuestionBankAddQuestionsDTO dto);

    void removeQuestion(Long bankId, Long questionId);
}
