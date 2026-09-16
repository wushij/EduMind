package com.edumind.question.api;

import com.edumind.question.dto.question.QuestionBatchCreateDTO;
import com.edumind.question.vo.question.QuestionBatchSaveVO;

/**
 * 题库跨模块写入公开 API
 */
public interface QuestionCommandApi {

    QuestionBatchSaveVO batchSave(QuestionBatchCreateDTO dto);
}
