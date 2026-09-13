package com.edumind.ai.agent.tool;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ToolRegistry {

    private final Map<String, AgentTool> tools = new HashMap<>();

    public ToolRegistry(List<AgentTool> toolList) {
        for (AgentTool tool : toolList) {
            tools.put(tool.name(), tool);
        }
    }

    public AgentTool get(String name) {
        return tools.get(name);
    }

    public int size() {
        return tools.size();
    }

    public List<AgentTool> all() {
        return List.copyOf(tools.values());
    }
}
