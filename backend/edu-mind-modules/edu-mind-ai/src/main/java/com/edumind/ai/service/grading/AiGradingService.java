package com.edumind.ai.service.grading;

import com.edumind.ai.dto.SubjectiveGradingDTO;
import com.edumind.ai.vo.SubjectiveGradingVO;

public interface AiGradingService {
    SubjectiveGradingVO gradeSubjective(SubjectiveGradingDTO dto);
}
