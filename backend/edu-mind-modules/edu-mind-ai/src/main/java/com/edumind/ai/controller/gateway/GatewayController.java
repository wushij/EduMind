package com.edumind.ai.controller.gateway;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.edumind.common.api.ApiResult;
import com.edumind.ai.service.gateway.GatewayManageService;
import com.edumind.ai.vo.gateway.GatewayMetricsVO;
import com.edumind.ai.vo.gateway.GatewayRouteVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
}
