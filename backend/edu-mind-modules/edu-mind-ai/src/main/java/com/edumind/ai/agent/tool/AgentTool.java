package com.edumind.ai.agent.tool;

import java.util.Map;

public interface AgentTool {
    String name();
    String description();
    Object execute(Map<String, Object> params);
}
