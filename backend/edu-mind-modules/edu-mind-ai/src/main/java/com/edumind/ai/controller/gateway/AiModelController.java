package com.edumind.ai.controller.gateway;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.edumind.common.api.ApiResult;
import com.edumind.ai.service.gateway.GatewayManageService;
import com.edumind.ai.vo.gateway.AiModelConfigVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping({"/api/system/ai-models", "/api/system/models"})
@RequiredArgsConstructor
public class AiModelController {

    private final GatewayManageService gatewayManageService;

    @SaCheckRole("ADMIN")
    @GetMapping
    public ApiResult<List<AiModelConfigVO>> listModels() {
        return ApiResult.success(gatewayManageService.listModels());
    }

    @SaCheckRole("ADMIN")
    @PutMapping("/{id}")
    public ApiResult<Void> updateModel(@PathVariable Long id, @RequestBody AiModelConfigVO vo) {
        gatewayManageService.updateModel(id, vo);
        return ApiResult.success();
    }
}
