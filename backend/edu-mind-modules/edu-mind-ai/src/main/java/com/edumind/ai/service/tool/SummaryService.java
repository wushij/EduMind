package com.edumind.ai.service.tool;

import com.edumind.ai.dto.tool.SummaryDTO;

public interface SummaryService {

    String summarize(SummaryDTO dto);
}
