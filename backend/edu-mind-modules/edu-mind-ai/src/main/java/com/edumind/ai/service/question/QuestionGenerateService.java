package com.edumind.ai.service.question;

import com.edumind.ai.dto.QuestionGenerateDTO;
import com.edumind.question.vo.question.QuestionVO;

import java.util.List;

public interface QuestionGenerateService {
    List<QuestionVO> generate(QuestionGenerateDTO dto);
}
