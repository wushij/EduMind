package com.edumind.ai.integration.llm;

import com.alibaba.fastjson2.JSON;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class MockLlmClient implements LlmClient {

    private final LlmProperties llmProperties;

    @Override
    public String chat(String systemPrompt, String userPrompt) {
        return "这是 Mock LLM 的回复。您的问题是：" + userPrompt;
    }

    @Override
    public String generateQuestions(String prompt, Map<String, Object> params) {
        int count = params.get("count") instanceof Number n ? n.intValue() : 3;
        List<Map<String, Object>> questions = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            Map<String, Object> question = new HashMap<>();
            question.put("type", "SINGLE_CHOICE");
            question.put("difficulty", 3);
            question.put("score", params.getOrDefault("scorePerQuestion", 5));
            question.put("stem", "Mock 题目 " + i + "：" + prompt);
            question.put("options", "[{\"key\":\"A\",\"content\":\"选项A\",\"isCorrect\":true},{\"key\":\"B\",\"content\":\"选项B\",\"isCorrect\":false}]");
            question.put("answer", "A");
            question.put("analysis", "Mock 解析");
            questions.add(question);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("questions", questions);
        return JSON.toJSONString(result);
    }

    @Override
    public void streamChat(String systemPrompt, String userPrompt, StreamCallback callback) {
        if (!Boolean.TRUE.equals(llmProperties.getMockEnabled())) {
            callback.onError("Mock LLM 未启用");
            return;
        }
        String reply = chat(systemPrompt, userPrompt);
        for (char c : reply.toCharArray()) {
            callback.onChunk(String.valueOf(c));
        }
        callback.onComplete();
    }
}
