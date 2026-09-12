package com.edumind.ai.vo.agent;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
public class AgentRunVO {
    private String runId;
    private String status;
    private List<AgentStepVO> steps = new ArrayList<>();
    private Map<String, Object> result;

    @Data
    public static class AgentStepVO {
        private Integer index;
        private String type;
        private String title;
        private String tool;
        private String status;
        private String outputPreview;
    }
}
