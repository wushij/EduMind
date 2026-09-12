package com.edumind.ai.agent.planner;

import com.alibaba.fastjson2.JSON;
import com.edumind.ai.gateway.AiGatewayFacade;
import com.edumind.ai.service.prompt.PromptService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AgentPlannerImpl implements AgentPlanner {

    private final AiGatewayFacade aiGatewayFacade;
    private final PromptService promptService;

    @Override
    public List<String> plan(String goal) {
        try {
            String system = promptService.getSystemPrompt("agent/planner");
            String reply = aiGatewayFacade.chat("AGENT", system, goal);
            String json = extractJsonArray(reply);
            List<String> steps = JSON.parseArray(json, String.class);
            if (steps != null && !steps.isEmpty()) {
                return steps;
            }
        } catch (Exception ignored) {
            // fallback below
        }
        return fallbackPlan(goal);
    }

    private String extractJsonArray(String reply) {
        if (reply == null) {
            return "[]";
        }
        int start = reply.indexOf('[');
        int end = reply.lastIndexOf(']');
        if (start >= 0 && end > start) {
            return reply.substring(start, end + 1);
        }
        return reply;
    }

    private List<String> fallbackPlan(String goal) {
        List<String> steps = new ArrayList<>();
        steps.add("识别意图与任务目标");
        steps.add("检索课程与知识点上下文");
        if (goal != null && (goal.contains("题") || goal.contains("出题"))) {
            steps.add("调用出题工具生成题目");
            steps.add("校验题目结构并保存");
        } else if (goal != null && goal.contains("批改")) {
            steps.add("调用批改工具评分");
        } else {
            steps.add("检索知识库并构建 RAG 上下文");
            steps.add("生成教学回答");
        }
        steps.add("汇总结果并返回");
        return steps;
    }
}
