package com.edumind.ai.service.teaching;

import com.edumind.ai.dto.teaching.LessonContentGenerateDTO;

public interface LessonContentGenerateService {

    String generateAndSaveDraft(LessonContentGenerateDTO dto);
}
