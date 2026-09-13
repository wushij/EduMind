package com.edumind.ai.dto.gateway;

import lombok.Data;

@Data
public class AiModelTestDTO {
    private String name;
    private String provider;
    private String configType;
    private String modelName;
    private String baseUrl;
    private String apiKey;
}
