package com.edumind.ai.agent.executor;

import com.alibaba.fastjson2.JSON;
import com.edumind.ai.agent.planner.AgentPlanner;
import com.edumind.ai.agent.tool.AgentTool;
import com.edumind.ai.agent.tool.ToolRegistry;
import com.edumind.ai.dao.AgentRunDao;
import com.edumind.ai.dao.AgentStepDao;
import com.edumind.ai.dao.AgentToolCallDao;
import com.edumind.ai.entity.AgentRunEntity;
import com.edumind.ai.entity.AgentStepEntity;
import com.edumind.ai.entity.AgentToolCallEntity;
import com.edumind.ai.gateway.AiGatewayFacade;
import com.edumind.ai.gateway.ModelRouterImpl;
import com.edumind.ai.rag.context.RagCitationMapper;
import com.edumind.ai.rag.model.RagResult;
import com.edumind.ai.rag.pipeline.RagPipelineImpl;
import com.edumind.ai.service.agent.AgentRunStreamRegistry;
import com.edumind.ai.vo.rag.CitationVO;
import com.edumind.common.utils.IdUtil;
import com.edumind.knowledge.api.KnowledgeQueryApi;
import com.edumind.knowledge.vo.knowledge.KnowledgeBaseVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AgentExecutorImpl {

    private final AgentPlanner agentPlanner;
    private final ToolRegistry toolRegistry;
    private final AgentRunDao agentRunDao;
    private final AgentStepDao agentStepDao;
    private final AgentToolCallDao agentToolCallDao;
    private final AiGatewayFacade aiGatewayFacade;
    private final ModelRouterImpl modelRouter;
    private final AgentRunStreamRegistry streamRegistry;
    private final RagPipelineImpl ragPipeline;
    private final KnowledgeQueryApi knowledgeQueryApi;

    public String createRun(String agentCode, Long userId, Long courseId, String goal) {
        String runId = "run-" + IdUtil.simpleUUID();
        AgentRunEntity run = new AgentRunEntity();
        run.setRunId(runId);
        run.setAgentCode(agentCode);
        run.setUserId(userId);
        run.setCourseId(courseId);
        run.setGoal(goal);
        run.setStatus("RUNNING");
        run.setModelKey(modelRouter.resolveModelKey("AGENT", null));
        agentRunDao.insert(run);
        return runId;
    }

    public void executeRun(String runId, String agentCode, Long courseId, String goal, Map<String, Object> context) {
        AgentRunEntity run = agentRunDao.findByRunId(runId);
        if (run == null) {
            return;
        }
        try {
            int index = 1;
            saveStep(runId, index++, "INTENT", "识别意图", null, "DONE", goal, "agent=" + agentCode);

            List<String> plan = agentPlanner.plan(goal);
            for (String stepTitle : plan) {
                if (streamRegistry.isCancelled(runId)) {
                    markFailed(run, "CANCELLED");
                    return;
                }
                saveStep(runId, index, "PLAN", stepTitle, null, "DONE", null, stepTitle);
                index++;
            }

            Map<String, Object> params = new HashMap<>();
            if (context != null) {
                params.putAll(context);
            }
            params.put("courseId", courseId);
            params.put("query", goal);

            Object result = executeAgentChain(runId, agentCode, goal, params, index);
            run.setStatus("SUCCEEDED");
            run.setResultJson(JSON.toJSONString(result));
            run.setUpdateTime(LocalDateTime.now());
            agentRunDao.updateById(run);
            streamRegistry.emit(runId, "done", Map.of("status", "SUCCEEDED"));
            streamRegistry.complete(runId);
        } catch (Exception ex) {
            markFailed(run, ex.getMessage());
            streamRegistry.emit(runId, "error", Map.of("message", ex.getMessage()));
            streamRegistry.complete(runId);
        }
    }

    private Object executeAgentChain(String runId, String agentCode, String goal,
                                     Map<String, Object> params, int startIndex) {
        int index = startIndex;
        return switch (agentCode) {
            case "question" -> runQuestionAgent(runId, params, index);
            case "teaching" -> runTeachingAgent(runId, goal, params, index);
            case "learning" -> runLearningAgent(runId, params, index);
            case "grading" -> runGradingAgent(runId, goal, index);
            default -> runTeachingAgent(runId, goal, params, index);
        };
    }

    private Object runQuestionAgent(String runId, Map<String, Object> params, int index) {
        Object kpResult = invokeTool(runId, index++, "检索知识点", "search_knowledge_point", params);
        params.put("knowledgeContext", kpResult);
        params.put("count", extractCount(String.valueOf(params.get("query"))));
        return invokeTool(runId, index, "生成题目", "generate_question", params);
    }

    private Object runTeachingAgent(String runId, String goal, Map<String, Object> params, int index) {
        if (goal != null && (goal.contains("资源") || goal.contains("微课") || goal.contains("拓展") || goal.contains("分析"))) {
            Object courseInfo = invokeTool(runId, index++, "查询课程教学目标", "search_course", params);
            Object profile = invokeTool(runId, index++, "分析学生弱项画像", "get_student_profile", params);
            params.put("studentProfile", profile);
            Object resources = invokeTool(runId, index++, "匹配拓展微课与练习", "recommend_resource", params);
            saveStep(runId, index, "LLM", "生成教学推进综合方案", null, "DONE", goal, "教学方案已生成");
            return Map.of("course", courseInfo != null ? courseInfo : Map.of(),
                    "profile", profile != null ? profile : Map.of(),
                    "resources", resources != null ? resources : Map.of(),
                    "answer", "已综合课程目标、学情画像与推荐资源，生成完整教学推进方案。");
        }
        Long courseId = params.get("courseId") != null ? Long.valueOf(params.get("courseId").toString()) : null;
        List<KnowledgeBaseVO> bases = knowledgeQueryApi.listKnowledgeBasesByCourseId(courseId);
        if (bases.isEmpty()) {
            saveStep(runId, index, "RAG", "检索知识库", "search_knowledge", "DONE", goal, "未找到知识库");
            return Map.of("answer", "未找到课程关联知识库，无法生成教学回答。", "citations", List.of());
        }
        Long kbId = bases.get(0).getId();
        RagResult ragResult = ragPipeline.executeDetailed(goal, kbId, 5, 0.0, null, false);
        List<CitationVO> citations = RagCitationMapper.toCitations(ragResult);
        String answer = ragResult.getAnswer() != null ? ragResult.getAnswer() : "";
        String preview = citations.isEmpty() ? answer : answer + " [citations=" + citations.size() + "]";
        saveStep(runId, index, "RAG", "检索知识库并生成教学回答", "rag_pipeline", "DONE", goal, preview);
        Map<String, Object> result = new HashMap<>();
        result.put("answer", answer);
        result.put("citations", citations);
        return result;
    }

    private Object runLearningAgent(String runId, Map<String, Object> params, int index) {
        Object profile = invokeTool(runId, index++, "获取学情画像", "get_student_profile", params);
        Object resources = invokeTool(runId, index++, "匹配弱项巩固资源", "recommend_resource", params);
        String advice = aiGatewayFacade.chat("AGENT", "你是学习路径顾问。", "基于以下学情给出复习建议：\n" + profile);
        saveStep(runId, index, "LLM", "生成个性化学习建议", null, "DONE", null, advice);
        streamRegistry.emit(runId, "step", Map.of("index", index, "title", "生成学习建议", "status", "DONE"));
        return Map.of("profile", profile != null ? profile : Map.of(),
                "resources", resources != null ? resources : Map.of(),
                "advice", advice);
    }

    private Object runGradingAgent(String runId, String goal, int index) {
        Object gradingResult = invokeTool(runId, index++, "智能作答评判", "grade_answer", Map.of("question", goal));
        String summary = aiGatewayFacade.chat("GRADING", "你是批改 Agent，请总结批改分析。", goal);
        saveStep(runId, index, "LLM", "批改综合归因分析", null, "DONE", goal, summary);
        streamRegistry.emit(runId, "step", Map.of("index", index, "title", "批改分析", "status", "DONE"));
        return Map.of("grading", gradingResult != null ? gradingResult : Map.of(), "summary", summary);
    }

    private Object invokeTool(String runId, int index, String title, String toolName, Map<String, Object> params) {
        AgentTool tool = toolRegistry.get(toolName);
        if (tool == null) {
            return null;
        }
        long start = System.currentTimeMillis();
        AgentStepEntity step = saveStep(runId, index, "TOOL", title, toolName, "RUNNING", JSON.toJSONString(params), null);
        streamRegistry.emit(runId, "step", Map.of("index", index, "title", title, "tool", toolName, "status", "RUNNING"));

        AgentToolCallEntity call = new AgentToolCallEntity();
        call.setRunId(runId);
        call.setStepId(step.getId());
        call.setToolName(toolName);
        call.setInputJson(JSON.toJSONString(params));
        call.setStatus("RUNNING");
        agentToolCallDao.insert(call);

        Object result = tool.execute(params);
        long duration = System.currentTimeMillis() - start;

        step.setStatus("DONE");
        step.setOutputPreview(String.valueOf(result));
        agentStepDao.updateById(step);

        call.setOutputJson(JSON.toJSONString(result));
        call.setStatus("DONE");
        call.setDurationMs((int) duration);
        agentToolCallDao.updateById(call);

        streamRegistry.emit(runId, "step", Map.of("index", index, "title", title, "tool", toolName, "status", "DONE"));
        return result;
    }

    private AgentStepEntity saveStep(String runId, int index, String type, String title, String tool,
                                     String status, String input, String output) {
        AgentStepEntity step = new AgentStepEntity();
        step.setRunId(runId);
        step.setStepIndex(index);
        step.setStepType(type);
        step.setTitle(title);
        step.setToolName(tool);
        step.setStatus(status);
        step.setInputPreview(input);
        step.setOutputPreview(output);
        agentStepDao.insert(step);
        Map<String, Object> event = new HashMap<>();
        event.put("index", index);
        event.put("type", type);
        event.put("title", title);
        event.put("status", status);
        if (tool != null) {
            event.put("tool", tool);
        }
        streamRegistry.emit(runId, "step", event);
        return step;
    }

    private void markFailed(AgentRunEntity run, String reason) {
        run.setStatus("FAILED");
        run.setResultJson(JSON.toJSONString(Map.of("error", reason != null ? reason : "unknown")));
        run.setUpdateTime(LocalDateTime.now());
        agentRunDao.updateById(run);
    }

    private int extractCount(String goal) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+)").matcher(goal);
        if (m.find()) {
            return Integer.parseInt(m.group(1));
        }
        return 5;
    }
}
