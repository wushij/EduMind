package com.edumind.statistics.service.learning;

import com.edumind.statistics.vo.learning.WrongBookDetailVO;
import com.edumind.statistics.vo.learning.WrongBookItemVO;
import com.edumind.statistics.vo.learning.WrongBookListVO;
import com.edumind.statistics.vo.learning.WrongBookOverviewVO;

public interface WrongBookService {

    WrongBookListVO list(Long studentId, Long courseId, int page, int pageSize,
                         String errorType, Long knowledgePointId, Integer status);

    WrongBookOverviewVO overview(Long studentId, Long courseId);

    WrongBookDetailVO detail(Long studentId, Long recordId);

    WrongBookItemVO diagnose(Long studentId, Long recordId);

    void markMastered(Long studentId, Long recordId);
}
