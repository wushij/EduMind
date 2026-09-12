package com.edumind.ai.vo.gateway;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AiModelConfigVO {
    private Long id;
    private String modelKey;
    private String provider;
    private Boolean enabled;
    private Integer priority;
    private String fallbackModelKey;
    private Integer maxTokens;
    private BigDecimal temperature;
}
