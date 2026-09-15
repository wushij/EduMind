package com.edumind.ai.api.impl;

import com.edumind.ai.api.AiGradingApi;
import com.edumind.ai.dto.SubjectiveGradingDTO;
import com.edumind.ai.service.grading.AiGradingService;
import com.edumind.ai.vo.SubjectiveGradingVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiGradingApiImpl implements AiGradingApi {

    private final AiGradingService aiGradingService;

    @Override
    public SubjectiveGradingVO gradeSubjective(SubjectiveGradingDTO dto) {
        return aiGradingService.gradeSubjective(dto);
    }
}
