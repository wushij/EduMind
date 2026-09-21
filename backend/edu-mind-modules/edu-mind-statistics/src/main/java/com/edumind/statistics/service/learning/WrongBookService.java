package com.edumind.statistics.service.learning;

import com.edumind.statistics.vo.learning.WrongBookDetailVO;
import com.edumind.statistics.vo.learning.WrongBookItemVO;
import com.edumind.statistics.vo.learning.WrongBookListVO;
import com.edumind.statistics.vo.learning.WrongBookOverviewVO;

import java.util.List;

public interface WrongBookService {

    WrongBookListVO list(Long studentId, Long courseId, int page, int pageSize,
                         String errorType, Long knowledgePointId, Integer status);

    WrongBookOverviewVO overview(Long studentId, Long courseId);

    WrongBookDetailVO detail(Long studentId, Long recordId);

    WrongBookItemVO diagnose(Long studentId, Long recordId);

    /**
     * 按需生成同构变式题，返回可展示的变式题摘要。
     *
     * @param regenerate false=已生成过则复用（避免重复消耗模型额度）；true=重新生成并替换旧的一批
     */
    List<WrongBookDetailVO.VariantQuestionSummaryVO> generateVariants(Long studentId, Long recordId,
                                                                      boolean regenerate);

    /** 学生中止 AI 归因诊断：服务端丢弃本次模型结果、不再落库 */
    void cancelAiDiagnosis(Long studentId, Long recordId);

    /** 学生中止变式题生成：服务端丢弃本次模型结果，且不写入题库 */
    void cancelAiVariants(Long studentId, Long recordId);

    void markMastered(Long studentId, Long recordId);
}
