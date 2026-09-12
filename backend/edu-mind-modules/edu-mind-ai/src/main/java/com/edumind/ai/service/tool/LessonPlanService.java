package com.edumind.ai.service.tool;

import com.edumind.ai.dto.tool.LessonPlanDTO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface LessonPlanService {

    String generate(LessonPlanDTO dto);

    SseEmitter streamGenerate(LessonPlanDTO dto);
}
