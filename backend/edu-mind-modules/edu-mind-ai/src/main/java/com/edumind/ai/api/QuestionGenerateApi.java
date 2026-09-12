package com.edumind.ai.api;

import com.edumind.ai.dto.QuestionGenerateDTO;
import com.edumind.question.vo.question.QuestionVO;

import java.util.List;

/**
 * 题目智能生成跨模块公开 API
 */
public interface QuestionGenerateApi {

    /**
     * 根据出题参数生成题目列表
     *
     * @param dto 出题参数 DTO
     * @return 生成的题目列表
     */
    List<QuestionVO> generate(QuestionGenerateDTO dto);
}
