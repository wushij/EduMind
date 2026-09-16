package com.edumind.ai.service.tool;

import com.edumind.ai.dto.tool.CourseObjectiveSuggestDTO;
import com.edumind.ai.dto.tool.CourseObjectiveSuggestResultVO;

public interface CourseObjectiveSuggestService {

    CourseObjectiveSuggestResultVO suggest(CourseObjectiveSuggestDTO dto);
}
