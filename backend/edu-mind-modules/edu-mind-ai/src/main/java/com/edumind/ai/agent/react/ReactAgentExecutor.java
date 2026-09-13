package com.edumind.ai.agent.react;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.edumind.ai.agent.tool.AgentTool;
import com.edumind.ai.agent.tool.ToolRegistry;
import com.edumind.ai.gateway.AiGatewayFacade;
import com.edumind.ai.service.prompt.PromptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReactAgentExecutor {

    public static final int MAX_STEPS = 5;

    private final AiGatewayFacade aiGatewayFacade;
    private final PromptService promptService;
    private final ToolRegistry toolRegistry;

    public Object execute(String runId,
                          String goal,
                          String agentCode,
                          Map<String, Object> context,
                          BiConsumer<Integer, Map<String, Object>> stepEmitter) {
        Map<String, Object> workingContext = new HashMap<>();
        if (context != null) {
            workingContext.putAll(context);
        }
        workingContext.put("agentCode", agentCode);
        workingContext.put("query", goal);

        StringBuilder observations = new StringBuilder();
        for (int step = 1; step <= MAX_STEPS; step++) {
            String prompt = buildPrompt(goal, observations.toString());
            String reply = aiGatewayFacade.chat("AGENT",
                    "你是 EduMind ReAct Agent，请严格输出 JSON 动作对象。", prompt);
            JSONObject action = parseAction(runId, step, reply);
            if (action == null) {
                throw new IllegalStateException("ReAct parse failed at step " + step);
            }

            String thought = action.getString("thought");
            String actionName = action.getString("action");
            emitStep(stepEmitter, step, "THOUGHT", thought, "RUNNING");

            if (!StringUtils.hasText(actionName) || "finish".equalsIgnoreCase(actionName)) {
                String finalAnswer = action.getString("finalAnswer");
                emitStep(stepEmitter, step, "FINISH", finalAnswer, "DONE");
                return Map.of(
                        "agentCode", agentCode,
                        "answer", finalAnswer != null ? finalAnswer : "",
                        "steps", step,
                        "executionMode", "react"
                );
            }

            AgentTool tool = toolRegistry.get(actionName);
            if (tool == null) {
                observations.append("\nStep ").append(step).append(" tool not found: ").append(actionName);
                continue;
            }

            Map<String, Object> actionInput = action.getJSONObject("actionInput") != null
                    ? action.getJSONObject("actionInput")
                    : new HashMap<>();
            actionInput.putIfAbsent("courseId", workingContext.get("courseId"));
            actionInput.putIfAbsent("query", goal);

            emitStep(stepEmitter, step, "ACTION", actionName, "RUNNING", actionName);
            Object toolResult = tool.execute(actionInput);
            observations.append("\nStep ").append(step)
                    .append(" Observation[").append(actionName).append("]: ")
                    .append(JSON.toJSONString(toolResult));
            emitStep(stepEmitter, step, "OBSERVE", JSON.toJSONString(toolResult), "DONE", actionName);
        }

        String summary = aiGatewayFacade.chat("AGENT", "你是 Agent 总结助手。", "根据以下观察生成最终回答：\n" + observations);
        return Map.of("agentCode", agentCode, "answer", summary, "steps", MAX_STEPS, "executionMode", "react");
    }

    private String buildPrompt(String goal, String observations) {
        List<Map<String, String>> tools = new ArrayList<>();
        for (AgentTool tool : toolRegistry.all()) {
            tools.add(Map.of("name", tool.name(), "description", tool.description()));
        }
        Map<String, String> vars = new HashMap<>();
        vars.put("tools", JSON.toJSONString(tools));
        vars.put("maxSteps", String.valueOf(MAX_STEPS));
        vars.put("goal", goal + (StringUtils.hasText(observations) ? "\n\n历史观察：\n" + observations : ""));
        return promptService.renderTemplate("agent/react", vars);
    }

    private JSONObject parseAction(String runId, int step, String reply) {
        if (!StringUtils.hasText(reply)) {
            log.warn("ReAct empty reply at runId={} step={}", runId, step);
            return null;
        }
        String payload = extractJsonPayload(reply);
        try {
            return JSON.parseObject(payload);
        } catch (Exception ex) {
            String preview = reply.length() > 200 ? reply.substring(0, 200) + "..." : reply;
            log.warn("Failed to parse ReAct action at runId={} step={}: {} | preview={}",
                    runId, step, ex.getMessage(), preview);
            return null;
        }
    }

    private String extractJsonPayload(String reply) {
        String trimmed = reply.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            int lastFence = trimmed.lastIndexOf("```");
            if (firstNewline > 0 && lastFence > firstNewline) {
                trimmed = trimmed.substring(firstNewline + 1, lastFence).trim();
            }
        }
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1);
        }
        return trimmed;
    }

    private void emitStep(BiConsumer<Integer, Map<String, Object>> stepEmitter,
                          int step, String type, String content, String status) {
        emitStep(stepEmitter, step, type, content, status, null);
    }

    private void emitStep(BiConsumer<Integer, Map<String, Object>> stepEmitter,
                          int step, String type, String content, String status, String tool) {
        if (stepEmitter == null) {
            return;
        }
        Map<String, Object> event = new HashMap<>();
        event.put("index", step);
        event.put("type", type);
        event.put("title", type);
        event.put("content", content);
        event.put("status", status);
        if (tool != null) {
            event.put("tool", tool);
        }
        stepEmitter.accept(step, event);
    }
}
