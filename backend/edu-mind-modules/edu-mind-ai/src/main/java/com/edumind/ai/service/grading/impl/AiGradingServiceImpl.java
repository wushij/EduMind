package com.edumind.ai.service.grading.impl;

import com.edumind.ai.dao.AiCallLogDao;
import com.edumind.ai.dto.SubjectiveGradingDTO;
import com.edumind.ai.entity.AiCallLogEntity;
import com.edumind.ai.gateway.AiGatewayFacade;
import com.edumind.ai.integration.llm.LlmProperties;
import com.edumind.ai.service.grading.AiGradingService;
import com.edumind.ai.service.prompt.PromptService;
import com.edumind.ai.vo.SubjectiveGradingVO;
import com.edumind.common.model.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AiGradingServiceImpl implements AiGradingService {

    private final AiGatewayFacade aiGatewayFacade;
    private final LlmProperties llmProperties;
    private final AiCallLogDao aiCallLogDao;
    private final PromptService promptService;

    @Override
    public SubjectiveGradingVO gradeSubjective(SubjectiveGradingDTO dto) {
        long start = System.currentTimeMillis();
        int maxScore = dto.getMaxScore() != null ? dto.getMaxScore() : 10;
        String userPrompt = """
                题目：%s
                参考答案：%s
                学生答案：%s
                满分：%d
                请给出 score 和 comment。
                """.formatted(
                dto.getQuestionStem(),
                dto.getReferenceAnswer(),
                dto.getStudentAnswer(),
                maxScore);

        String reply = aiGatewayFacade.chat("GRADING", promptService.getSystemPrompt("subjective_grading"), userPrompt);
        int score = estimateScore(reply, maxScore, dto.getStudentAnswer(), dto.getReferenceAnswer());
        String status = score >= maxScore * 0.6 ? "AUTO_GRADED" : "PENDING_REVIEW";

        logCall(start);

        return SubjectiveGradingVO.builder()
                .score(score)
                .maxScore(maxScore)
                .aiComment(reply)
                .status(status)
                .build();
    }

    private int estimateScore(String reply, int maxScore, String studentAnswer, String referenceAnswer) {
        if (!StringUtils.hasText(studentAnswer)) {
            return 0;
        }
        if (studentAnswer.trim().equalsIgnoreCase(referenceAnswer != null ? referenceAnswer.trim() : "")) {
            return maxScore;
        }
        return Math.max(1, (int) (maxScore * 0.7));
    }

    private void logCall(long start) {
        AiCallLogEntity log = new AiCallLogEntity();
        log.setUserId(UserContext.getUserId());
        log.setModel(llmProperties.getModel());
        log.setScene("subjective_grading");
        log.setLatencyMs((int) (System.currentTimeMillis() - start));
        aiCallLogDao.insert(log);
    }
}
