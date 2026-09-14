package com.edumind.ai.controller.gateway;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.edumind.ai.dto.gateway.AiModelSaveDTO;
import com.edumind.ai.dto.gateway.AiModelTestDTO;
import com.edumind.ai.service.gateway.AiModelManageService;
import com.edumind.ai.vo.gateway.AiModelConfigVO;
import com.edumind.ai.vo.gateway.AiModelTestResultVO;
import com.edumind.ai.vo.gateway.AiProviderPresetsResponseVO;
import com.edumind.common.annotation.OperationLog;
import com.edumind.common.api.ApiResult;
import com.edumind.common.enums.BusinessType;
import com.edumind.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/system/ai-models", "/api/system/models"})
@RequiredArgsConstructor
public class AiModelController {

    private final AiModelManageService aiModelManageService;

    @SaCheckRole("ADMIN")
    @GetMapping("/provider-presets")
    public ApiResult<AiProviderPresetsResponseVO> providerPresets() {
        return ApiResult.success(aiModelManageService.getProviderPresets());
    }

    @SaCheckRole("ADMIN")
    @GetMapping
    public ApiResult<Map<String, Object>> listModels(
            @RequestParam(required = false) String configType,
            @RequestParam(required = false) String status) {
        List<AiModelConfigVO> list = aiModelManageService.listModels(configType, status);
        Map<String, Object> page = new HashMap<>();
        page.put("records", list);
        page.put("list", list);
        page.put("total", list.size());
        page.put("page", 1);
        page.put("size", list.size());
        return ApiResult.success(page);
    }

    @SaCheckRole("ADMIN")
    @PostMapping
    @OperationLog(module = "模型运维", title = "新增AI模型配置", businessType = BusinessType.INSERT)
    public ApiResult<AiModelConfigVO> createModel(@RequestBody AiModelSaveDTO dto) {
        return ApiResult.success(aiModelManageService.createModel(dto));
    }

    @SaCheckRole("ADMIN")
    @PutMapping("/{name}")
    @OperationLog(module = "模型运维", title = "修改AI模型配置", businessType = BusinessType.UPDATE)
    public ApiResult<Map<String, Object>> updateModel(@PathVariable String name, @RequestBody AiModelSaveDTO dto) {
        aiModelManageService.updateModel(name, dto);
        return ApiResult.success(Map.of("updated", true, "name", name));
    }

    @SaCheckRole("ADMIN")
    @DeleteMapping("/{name}")
    @OperationLog(module = "模型运维", title = "删除AI模型配置", businessType = BusinessType.DELETE)
    public ApiResult<Map<String, Object>> deleteModel(@PathVariable String name) {
        aiModelManageService.deleteModel(name);
        return ApiResult.success(Map.of("deleted", true, "name", name));
    }

    @SaCheckRole("ADMIN")
    @PostMapping("/{name}/default")
    @OperationLog(module = "模型运维", title = "设置默认AI模型", businessType = BusinessType.GRANT)
    public ApiResult<Map<String, Object>> setDefaultModel(@PathVariable String name) {
        aiModelManageService.setDefaultModel(name);
        return ApiResult.success(Map.of("is_default", true, "name", name));
    }

    @SaCheckRole("ADMIN")
    @PostMapping("/{name}/test")
    public ApiResult<AiModelTestResultVO> testSavedModel(@PathVariable String name) {
        try {
            return ApiResult.success(aiModelManageService.testSavedModel(name));
        } catch (IllegalArgumentException | BusinessException ex) {
            throw new BusinessException(ex.getMessage());
        }
    }

    @SaCheckRole("ADMIN")
    @PostMapping("/test")
    public ApiResult<AiModelTestResultVO> testDraftModel(@RequestBody AiModelTestDTO dto) {
        try {
            return ApiResult.success(aiModelManageService.testDraftModel(dto));
        } catch (IllegalArgumentException | BusinessException ex) {
            throw new BusinessException(ex.getMessage());
        }
    }

    /** 兼容旧 PUT /{id} 更新接口 */
    @SaCheckRole("ADMIN")
    @PutMapping("/{id:\\d+}")
    public ApiResult<Void> updateModelById(@PathVariable Long id, @RequestBody AiModelConfigVO vo) {
        if (vo.getName() == null) {
            throw new BusinessException("配置名称不能为空");
        }
        AiModelSaveDTO dto = new AiModelSaveDTO();
        dto.setName(vo.getName());
        dto.setProvider(vo.getProvider());
        dto.setConfigType(vo.getConfigType());
        dto.setModelName(vo.getModelName());
        dto.setBaseUrl(vo.getBaseUrl());
        dto.setApiKey(vo.getApiKey());
        dto.setTemperature(vo.getTemperature());
        dto.setReasoningEffort(vo.getReasoningEffort());
        dto.setDimension(vo.getDimension());
        dto.setStatus(vo.getStatus());
        dto.setIsDefault(vo.getIsDefault());
        dto.setSortOrder(vo.getSortOrder());
        aiModelManageService.updateModel(vo.getName(), dto);
        return ApiResult.success();
    }
}
