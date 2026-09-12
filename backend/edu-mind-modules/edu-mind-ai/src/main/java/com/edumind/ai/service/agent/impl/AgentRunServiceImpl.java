package com.edumind.ai.service.agent.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.edumind.ai.agent.executor.AgentExecutorImpl;
import com.edumind.ai.agent.tool.ToolRegistry;
import com.edumind.ai.dao.AgentRunDao;
import com.edumind.ai.dao.AgentStepDao;
import com.edumind.ai.dto.agent.AgentRunCreateDTO;
import com.edumind.ai.entity.AgentRunEntity;
import com.edumind.ai.entity.AgentStepEntity;
import com.edumind.ai.gateway.ModelRouterImpl;
import com.edumind.ai.integration.llm.LlmProperties;
import com.edumind.ai.service.agent.AgentRunService;
import com.edumind.ai.service.agent.AgentRunStreamRegistry;
import com.edumind.ai.vo.agent.AgentRunVO;
import com.edumind.ai.vo.agent.AgentVO;
import com.edumind.common.model.LoginUser;
import com.edumind.common.model.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class AgentRunServiceImpl implements AgentRunService {

    private final AgentExecutorImpl agentExecutor;
    private final AgentRunDao agentRunDao;
    private final AgentStepDao agentStepDao;
    private final ToolRegistry toolRegistry;
    private final ModelRouterImpl modelRouter;
    private final AgentRunStreamRegistry streamRegistry;
    private final LlmProperties llmProperties;

    @Override
    public List<AgentVO> listAgents() {
        List<AgentVO> agents = new ArrayList<>();
        agents.add(buildAgent("teaching", "教学 Agent"));
        agents.add(buildAgent("question", "出题 Agent"));
        agents.add(buildAgent("grading", "批改 Agent"));
        agents.add(buildAgent("learning", "学习 Agent"));
        return agents;
    }

    private AgentVO buildAgent(String code, String name) {
        AgentVO vo = new AgentVO();
        vo.setCode(code);
        vo.setName(name);
        vo.setStatus("ACTIVE");
        vo.setModelKey(modelRouter.resolveModelKey("AGENT", null));
        vo.setToolCount(toolRegistry.size());
        long total = agentRunDao.countByAgentCode(code);
        long success = agentRunDao.countSucceededByAgentCode(code);
        vo.setTotalRuns(total);
        vo.setSuccessRate(total == 0 ? 1.0 : success * 1.0 / total);
        return vo;
    }

    @Override
    public String startRun(AgentRunCreateDTO dto, Long userId) {
        String runId = agentExecutor.createRun(dto.getAgentCode(), userId, dto.getCourseId(), dto.getGoal());
        LoginUser loginUser = UserContext.get();
        if (loginUser == null && userId != null) {
            loginUser = LoginUser.builder().id(userId).build();
        }
        final LoginUser asyncUser = loginUser;
        CompletableFuture.runAsync(() -> {
            try {
                if (asyncUser != null) {
                    UserContext.set(asyncUser);
                }
                agentExecutor.executeRun(
                        runId, dto.getAgentCode(), dto.getCourseId(), dto.getGoal(), dto.getContext());
            } finally {
                UserContext.clear();
            }
        });
        return runId;
    }

    @Override
    public AgentRunVO getRun(String runId) {
        AgentRunEntity run = agentRunDao.findByRunId(runId);
        if (run == null) {
            return null;
        }
        AgentRunVO vo = new AgentRunVO();
        vo.setRunId(run.getRunId());
        vo.setStatus(run.getStatus());
        if (run.getResultJson() != null) {
            vo.setResult(JSON.parseObject(run.getResultJson(), new TypeReference<Map<String, Object>>() {}));
        }
        for (AgentStepEntity step : agentStepDao.listByRunId(runId)) {
            AgentRunVO.AgentStepVO s = new AgentRunVO.AgentStepVO();
            s.setIndex(step.getStepIndex());
            s.setType(step.getStepType());
            s.setTitle(step.getTitle());
            s.setTool(step.getToolName());
            s.setStatus(step.getStatus());
            s.setOutputPreview(step.getOutputPreview());
            vo.getSteps().add(s);
        }
        return vo;
    }

    @Override
    public SseEmitter streamRun(String runId) {
        SseEmitter emitter = new SseEmitter(llmProperties.getTimeoutMs().longValue());
        streamRegistry.register(runId, emitter);
        AgentRunVO run = getRun(runId);
        if (run != null && !"RUNNING".equals(run.getStatus())) {
            try {
                emitter.send(SseEmitter.event().name("done").data(Map.of("status", run.getStatus())));
                emitter.complete();
            } catch (Exception ignored) {
                emitter.complete();
            }
        }
        return emitter;
    }

    @Override
    public void cancelStream(String runId) {
        streamRegistry.cancel(runId);
    }
}
