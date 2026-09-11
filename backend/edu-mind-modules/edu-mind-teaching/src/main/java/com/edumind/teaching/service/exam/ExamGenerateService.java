package com.edumind.teaching.service.exam;

import com.edumind.teaching.dto.exam.ExamGenerateDTO;
import com.edumind.teaching.vo.exam.ExamPreviewVO;

public interface ExamGenerateService {
    ExamPreviewVO generate(ExamGenerateDTO dto);
}
