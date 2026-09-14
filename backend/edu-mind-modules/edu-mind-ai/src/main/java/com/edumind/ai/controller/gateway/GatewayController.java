package com.edumind.ai.controller.gateway;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.edumind.ai.service.gateway.GatewayManageService;
import com.edumind.ai.vo.audit.AiCallLogVO;
import com.edumind.ai.vo.gateway.GatewayMetricsVO;
import com.edumind.ai.vo.gateway.GatewayRouteVO;
import com.edumind.common.api.ApiResult;
import com.edumind.common.api.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/system/gateway")
@RequiredArgsConstructor
public class GatewayController {

    private final GatewayManageService gatewayManageService;

    @SaCheckRole("ADMIN")
    @GetMapping("/metrics")
    public ApiResult<GatewayMetricsVO> metrics(@RequestParam(defaultValue = "24h") String range) {
        return ApiResult.success(gatewayManageService.getMetrics(range));
    }

    @SaCheckRole("ADMIN")
    @GetMapping("/routes")
    public ApiResult<List<GatewayRouteVO>> listRoutes() {
        return ApiResult.success(gatewayManageService.listRoutes());
    }

    @SaCheckRole("ADMIN")
    @PutMapping("/routes")
    public ApiResult<Void> updateRoutes(@RequestBody List<GatewayRouteVO> routes) {
        gatewayManageService.updateRoutes(routes);
        return ApiResult.success();
    }

    @SaCheckRole("ADMIN")
    @GetMapping("/logs")
    public ApiResult<PageResult<AiCallLogVO>> logs(
            @RequestParam(required = false) String scene,
            @RequestParam(required = false) String model,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long pageSize) {
        return ApiResult.success(gatewayManageService.listGatewayLogs(scene, model, page, pageSize));
    }

    @SaCheckRole("ADMIN")
    @PostMapping("/circuit/reset")
    public ApiResult<Void> resetCircuit(@RequestParam(required = false) String modelKey) {
        gatewayManageService.resetCircuit(modelKey);
        return ApiResult.success();
    }
}
