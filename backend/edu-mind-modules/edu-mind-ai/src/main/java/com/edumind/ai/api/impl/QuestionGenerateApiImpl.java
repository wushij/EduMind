package com.edumind.ai.api.impl;

import com.edumind.ai.api.QuestionGenerateApi;
import com.edumind.ai.dto.QuestionGenerateDTO;
import com.edumind.ai.service.question.QuestionGenerateService;
import com.edumind.question.vo.question.QuestionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class QuestionGenerateApiImpl implements QuestionGenerateApi {

    private final QuestionGenerateService questionGenerateService;

    @Override
    public List<QuestionVO> generate(QuestionGenerateDTO dto) {
        return questionGenerateService.generate(dto);
    }
}
