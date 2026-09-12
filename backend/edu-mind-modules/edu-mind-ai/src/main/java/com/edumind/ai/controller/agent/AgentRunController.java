package com.edumind.ai.controller.agent;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.edumind.common.api.ApiResult;
import com.edumind.common.model.UserContext;
import com.edumind.ai.dto.agent.AgentRunCreateDTO;
import com.edumind.ai.service.agent.AgentRunService;
import com.edumind.ai.vo.agent.AgentRunVO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@RestController
@RequestMapping("/api/ai/agent/runs")
@RequiredArgsConstructor
public class AgentRunController {

    private final AgentRunService agentRunService;

    @SaCheckPermission("ai:tool:use")
    @PostMapping
    public ApiResult<Map<String, String>> startRun(@RequestBody AgentRunCreateDTO dto) {
        String runId = agentRunService.startRun(dto, UserContext.getUserId());
        return ApiResult.success(Map.of("runId", runId, "status", "RUNNING"));
    }

    @SaCheckPermission("ai:tool:use")
    @GetMapping("/{runId}")
    public ApiResult<AgentRunVO> getRun(@PathVariable String runId) {
        return ApiResult.success(agentRunService.getRun(runId));
    }

    @SaCheckPermission("ai:tool:use")
    @GetMapping(value = "/{runId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamRun(@PathVariable String runId) {
        return agentRunService.streamRun(runId);
    }

    @SaCheckPermission("ai:tool:use")
    @DeleteMapping("/{runId}/stream")
    public ApiResult<Void> cancelStream(@PathVariable String runId) {
        agentRunService.cancelStream(runId);
        return ApiResult.success();
    }
}
