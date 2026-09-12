package com.edumind.ai.service.agent;

import com.edumind.ai.dto.agent.AgentRunCreateDTO;
import com.edumind.ai.vo.agent.AgentRunVO;
import com.edumind.ai.vo.agent.AgentVO;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

public interface AgentRunService {

    List<AgentVO> listAgents();

    String startRun(AgentRunCreateDTO dto, Long userId);

    AgentRunVO getRun(String runId);

    SseEmitter streamRun(String runId);

    void cancelStream(String runId);
}
