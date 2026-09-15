package com.edumind.ai.api;

import com.edumind.ai.dto.SubjectiveGradingDTO;
import com.edumind.ai.vo.SubjectiveGradingVO;

/**
 * AI 主观题批改跨模块公开 API
 */
public interface AiGradingApi {

    SubjectiveGradingVO gradeSubjective(SubjectiveGradingDTO dto);
}
