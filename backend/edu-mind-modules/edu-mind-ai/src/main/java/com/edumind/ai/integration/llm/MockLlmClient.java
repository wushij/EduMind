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
    private final String modelKey;

    public MockLlmClient(LlmProperties llmProperties) {
        this(llmProperties, "mock");
    }

    @Override
    public String chat(String systemPrompt, String userPrompt) {
        if (isReactAgentCall(systemPrompt)) {
            return buildReactAction(userPrompt);
        }
        if (systemPrompt != null && systemPrompt.contains("总结助手")) {
            return "基于工具观察结果，已生成教学推进建议。";
        }
        return "[" + modelKey + "] 这是 Mock LLM 的回复。您的问题是：" + userPrompt;
    }

    private boolean isReactAgentCall(String systemPrompt) {
        return systemPrompt != null && systemPrompt.contains("ReAct Agent");
    }

    private String buildReactAction(String userPrompt) {
        String goalSection = extractGoalSection(userPrompt);
        String observations = extractObservationHistory(userPrompt);
        boolean questionFlow = goalSection.contains("单选题")
                || goalSection.contains("多选题")
                || (goalSection.contains("生成") && goalSection.contains("题"));

        Map<String, Object> action = new HashMap<>();
        if (questionFlow) {
            if (!hasObservation(observations, "search_knowledge_point")) {
                action.put("thought", "检索相关知识点");
                action.put("action", "search_knowledge_point");
                action.put("actionInput", Map.of("courseId", 102));
            } else if (!hasObservation(observations, "generate_question")) {
                action.put("thought", "生成题目");
                action.put("action", "generate_question");
                action.put("actionInput", Map.of("courseId", 102, "count", 5));
            } else {
                action.put("thought", "题目已生成");
                action.put("action", "finish");
                action.put("finalAnswer", "已根据知识点生成练习题目。");
            }
            return JSON.toJSONString(action);
        }

        if (!hasObservation(observations, "get_student_profile")) {
            action.put("thought", "先获取学生学情画像");
            action.put("action", "get_student_profile");
            action.put("actionInput", Map.of("studentId", 1001, "courseId", 102));
        } else if (!hasObservation(observations, "recommend_resource")) {
            action.put("thought", "基于弱项推荐巩固资源");
            action.put("action", "recommend_resource");
            action.put("actionInput", Map.of("courseId", 102, "limit", 3));
        } else {
            action.put("thought", "信息已足够，生成最终回答");
            action.put("action", "finish");
            action.put("finalAnswer", "已综合分析学情画像并推荐匹配资源，建议按推荐顺序完成巩固练习。");
        }
        return JSON.toJSONString(action);
    }

    /** 仅解析 ReAct 模板中的「历史观察」段，避免工具清单里的 tool name 误判为已执行。 */
    private String extractObservationHistory(String userPrompt) {
        if (userPrompt == null) {
            return "";
        }
        int marker = userPrompt.indexOf("历史观察：");
        return marker >= 0 ? userPrompt.substring(marker) : "";
    }

    private String extractGoalSection(String userPrompt) {
        if (userPrompt == null) {
            return "";
        }
        int marker = userPrompt.indexOf("任务目标：");
        return marker >= 0 ? userPrompt.substring(marker) : userPrompt;
    }

    private boolean hasObservation(String observations, String toolName) {
        return observations.contains("Observation[" + toolName + "]");
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
