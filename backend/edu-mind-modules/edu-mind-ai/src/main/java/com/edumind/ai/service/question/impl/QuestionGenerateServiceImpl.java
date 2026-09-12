package com.edumind.ai.service.question.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.edumind.ai.dao.AiCallLogDao;
import com.edumind.ai.dto.QuestionGenerateDTO;
import com.edumind.ai.entity.AiCallLogEntity;
import com.edumind.ai.gateway.AiGatewayFacade;
import com.edumind.ai.integration.llm.LlmProperties;
import com.edumind.ai.service.prompt.PromptService;
import com.edumind.ai.service.question.QuestionGenerateService;
import com.edumind.common.exception.BusinessException;
import com.edumind.common.model.UserContext;
import com.edumind.infrastructure.redis.cache.AiSessionCacheService;
import com.edumind.question.vo.question.QuestionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class QuestionGenerateServiceImpl implements QuestionGenerateService {

    private static final long GENERATING_TTL_SECONDS = 120L;

    private final AiGatewayFacade aiGatewayFacade;
    private final LlmProperties llmProperties;
    private final AiCallLogDao aiCallLogDao;
    private final PromptService promptService;
    private final AiSessionCacheService aiSessionCacheService;

    @Override
    public List<QuestionVO> generate(QuestionGenerateDTO dto) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException("未登录");
        }
        if (!aiSessionCacheService.tryStartGenerating("question", userId, GENERATING_TTL_SECONDS)) {
            throw new BusinessException("题目正在生成中，请稍候");
        }
        try {
            long start = System.currentTimeMillis();
            Map<String, Object> params = new HashMap<>();
            params.put("courseId", dto.getCourseId());
            params.put("count", dto.getCount());
            params.put("scorePerQuestion", dto.getScorePerQuestion() != null ? dto.getScorePerQuestion() : 5);
            params.put("questionTypes", dto.getQuestionTypes());
            params.put("difficulty", dto.getDifficulty());

            String userPrompt = "课程ID=" + dto.getCourseId()
                    + ", 知识点=" + dto.getKnowledgePointIds()
                    + ", 题型=" + dto.getQuestionTypes()
                    + ", 数量=" + dto.getCount();

            String json = aiGatewayFacade.generateQuestions("AGENT", null,
                    promptService.getSystemPrompt("question_generate") + "\n" + userPrompt, params);

            List<QuestionVO> result = parseQuestions(json, dto);
            logCall("question_generate", start);
            return result;
        } finally {
            aiSessionCacheService.finishGenerating("question", userId);
        }
    }

    private List<QuestionVO> parseQuestions(String json, QuestionGenerateDTO dto) {
        List<QuestionVO> list = new ArrayList<>();
        JSONObject root = JSON.parseObject(json);
        JSONArray questions = root.getJSONArray("questions");
        if (questions == null) {
            return list;
        }
        int scoreEach = dto.getScorePerQuestion() != null ? dto.getScorePerQuestion() : 5;
        for (int i = 0; i < questions.size(); i++) {
            JSONObject item = questions.getJSONObject(i);
            QuestionVO vo = new QuestionVO();
            vo.setCourseId(dto.getCourseId());
            vo.setType(item.getString("type"));
            vo.setStem(item.getString("stem"));
            vo.setOptions(item.getString("options"));
            vo.setAnswer(item.getString("answer"));
            vo.setAnalysis(item.getString("analysis"));
            vo.setDifficulty(item.getInteger("difficulty") != null ? item.getInteger("difficulty") : 3);
            vo.setScore(item.getInteger("score") != null ? item.getInteger("score") : scoreEach);
            list.add(vo);
        }
        return list;
    }

    private void logCall(String scene, long start) {
        AiCallLogEntity log = new AiCallLogEntity();
        log.setUserId(UserContext.getUserId());
        log.setModel(llmProperties.getModel());
        log.setScene(scene);
        log.setLatencyMs((int) (System.currentTimeMillis() - start));
        aiCallLogDao.insert(log);
    }
}
