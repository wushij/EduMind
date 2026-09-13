package com.edumind.ai.dto.gateway;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AiModelSaveDTO {
    private String name;
    private String provider;
    private String configType;
    private String modelName;
    private String baseUrl;
    private String apiKey;
    private BigDecimal temperature;
    private String reasoningEffort;
    private Integer dimension;
    private String status;
    private Boolean isDefault;
    private Integer sortOrder;
}
