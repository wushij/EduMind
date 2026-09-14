package com.edumind.ai.service.usage;

import com.edumind.ai.vo.usage.PersonalAiUsageVO;

public interface PersonalAiUsageService {

    PersonalAiUsageVO getMyUsage(int logDays, long pageNum, long pageSize);
}
