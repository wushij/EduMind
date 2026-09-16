package com.edumind.ai.service.tool;

import com.edumind.ai.dto.tool.CourseDescriptionSuggestDTO;

import com.edumind.ai.dto.tool.CourseDescriptionSuggestResultVO;

public interface CourseDescriptionSuggestService {

    CourseDescriptionSuggestResultVO suggest(CourseDescriptionSuggestDTO dto);
}
