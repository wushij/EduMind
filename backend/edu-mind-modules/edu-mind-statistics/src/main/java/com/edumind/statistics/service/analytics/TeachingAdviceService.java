package com.edumind.statistics.service.analytics;

import com.edumind.statistics.dto.analytics.TeachingAdviceRequestDTO;
import com.edumind.statistics.vo.analytics.TeachingAdviceVO;

public interface TeachingAdviceService {

    TeachingAdviceVO generateAdvice(TeachingAdviceRequestDTO request);
}
