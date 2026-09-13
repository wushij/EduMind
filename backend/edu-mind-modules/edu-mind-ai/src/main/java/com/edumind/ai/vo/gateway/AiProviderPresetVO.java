package com.edumind.ai.vo.gateway;

import lombok.Data;

import java.util.List;

@Data
public class AiProviderPresetVO {
    private String label;
    private String modelName;
    private String baseUrl;
    private String portalUrl;
    private String protocol;
    private List<String> modelOptions;
}
