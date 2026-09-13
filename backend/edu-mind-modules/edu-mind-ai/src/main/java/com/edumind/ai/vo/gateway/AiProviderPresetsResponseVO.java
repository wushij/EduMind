package com.edumind.ai.vo.gateway;

import lombok.Data;

import java.util.Map;

@Data
public class AiProviderPresetsResponseVO {
    private String catalogVersion;
    private Map<String, AiProviderPresetVO> chat;
    private Map<String, AiProviderPresetVO> embedding;
}
