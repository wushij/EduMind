package com.edumind.ai.dto.agent;

import lombok.Data;

import java.util.Map;

@Data
public class AgentRunCreateDTO {
    private String agentCode;
    private String goal;
    private Long courseId;
    private Map<String, Object> context;
}
