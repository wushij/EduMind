package com.edumind.question.api.impl;

import com.edumind.question.api.QuestionCommandApi;
import com.edumind.question.dto.question.QuestionBatchCreateDTO;
import com.edumind.question.service.question.QuestionService;
import com.edumind.question.vo.question.QuestionBatchSaveVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class QuestionCommandApiImpl implements QuestionCommandApi {

    private final QuestionService questionService;

    @Override
    public QuestionBatchSaveVO batchSave(QuestionBatchCreateDTO dto) {
        return questionService.batchSave(dto);
    }
}
