package com.edumind.statistics.service.learning;

import com.edumind.statistics.dto.learning.AiPracticeGradeDTO;
import com.edumind.statistics.dto.learning.AiPracticeStartDTO;
import com.edumind.statistics.dto.learning.AiPracticeSubmitDTO;
import com.edumind.statistics.vo.learning.AiPracticeGradeVO;
import com.edumind.statistics.vo.learning.AiPracticeSessionVO;
import com.edumind.statistics.vo.learning.AiPracticeSubmitVO;
import com.edumind.statistics.vo.learning.StudentWrongQuestionVO;

public interface AIPracticeService {

    StudentWrongQuestionVO listWrongQuestions(Long studentId, Long courseId, int page, int pageSize);

    AiPracticeSessionVO startPractice(Long studentId, AiPracticeStartDTO dto);

    AiPracticeGradeVO gradeAnswer(Long studentId, AiPracticeGradeDTO dto);

    AiPracticeSubmitVO submitPractice(Long studentId, AiPracticeSubmitDTO dto);
}
