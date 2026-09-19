package com.edumind.ai.service.tool;

import com.edumind.ai.dto.tool.CourseKnowledgePointSuggestDTO;
import com.edumind.ai.dto.tool.CourseKnowledgePointSuggestResultVO;

public interface CourseKnowledgePointSuggestService {

    CourseKnowledgePointSuggestResultVO suggest(CourseKnowledgePointSuggestDTO dto);
}
