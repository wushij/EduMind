package com.edumind.ai.service.question;

import com.edumind.ai.dto.QuestionGenerateDTO;
import com.edumind.question.vo.question.QuestionVO;

import java.util.List;

public interface QuestionGenerateService {
    List<QuestionVO> generate(QuestionGenerateDTO dto);

    /** 释放当前用户命题生成互斥锁（前端中止 HTTP 请求时须调用，否则会出现「正在生成中」） */
    void cancelActiveGeneration();
}
