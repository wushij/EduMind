package com.edumind.ai.service.gateway;

import com.edumind.ai.vo.gateway.AiProviderPresetsResponseVO;

import java.util.List;

public interface AiModelManageService {

    AiProviderPresetsResponseVO getProviderPresets();

    List<com.edumind.ai.vo.gateway.AiModelConfigVO> listModels(String configType, String status);

    com.edumind.ai.vo.gateway.AiModelConfigVO createModel(com.edumind.ai.dto.gateway.AiModelSaveDTO dto);

    void updateModel(String configName, com.edumind.ai.dto.gateway.AiModelSaveDTO dto);

    void deleteModel(String configName);

    void setDefaultModel(String configName);

    com.edumind.ai.vo.gateway.AiModelTestResultVO testSavedModel(String configName);

    com.edumind.ai.vo.gateway.AiModelTestResultVO testDraftModel(com.edumind.ai.dto.gateway.AiModelTestDTO dto);

    List<com.edumind.ai.vo.gateway.AiModelConfigVO> listEnabledChatModels();
}
