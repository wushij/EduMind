package com.edumind.ai.vo.agent;

import lombok.Data;

@Data
public class AgentVO {
    private String code;
    private String name;
    private String status;
    private String modelKey;
    private Integer toolCount;
    private Long totalRuns;
    private Double successRate;
}
