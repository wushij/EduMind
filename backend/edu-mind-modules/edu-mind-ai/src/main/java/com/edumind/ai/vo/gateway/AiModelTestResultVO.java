package com.edumind.ai.vo.gateway;

import lombok.Data;

@Data
public class AiModelTestResultVO {
    private Boolean success;
    private Long latencyMs;
    private Long latency;

    public static AiModelTestResultVO ok(long latencyMs) {
        AiModelTestResultVO vo = new AiModelTestResultVO();
        vo.setSuccess(true);
        vo.setLatencyMs(latencyMs);
        vo.setLatency(latencyMs);
        return vo;
    }
}
