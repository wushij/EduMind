package com.edumind.ai.controller.agent;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.edumind.common.api.ApiResult;
import com.edumind.ai.service.agent.AgentRunService;
import com.edumind.ai.vo.agent.AgentVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ai/agents")
@RequiredArgsConstructor
public class AgentController {

    private final AgentRunService agentRunService;

    @SaCheckPermission("ai:tool:use")
    @GetMapping
    public ApiResult<List<AgentVO>> listAgents() {
        return ApiResult.success(agentRunService.listAgents());
    }
}
