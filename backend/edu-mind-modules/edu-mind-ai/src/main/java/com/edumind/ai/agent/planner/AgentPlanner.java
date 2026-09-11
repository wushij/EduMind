package com.edumind.ai.agent.planner;

import java.util.List;

/**
 * 智能体规划器（基于 ReAct / Plan-and-Solve 范式拆解复杂教学任务）
 */
public interface AgentPlanner {
    List<String> plan(String goal);
}
