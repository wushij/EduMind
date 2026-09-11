package com.edumind.statistics.controller.dashboard;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.edumind.common.api.ApiResult;
import com.edumind.statistics.service.dashboard.DashboardService;
import com.edumind.statistics.vo.dashboard.DashboardSummaryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @SaCheckPermission("course:view")
    @GetMapping("/summary")
    public ApiResult<DashboardSummaryVO> getSummary() {
        return ApiResult.success(dashboardService.getSummary());
    }
}
