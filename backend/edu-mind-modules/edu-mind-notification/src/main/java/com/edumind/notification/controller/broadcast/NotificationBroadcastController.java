package com.edumind.notification.controller.broadcast;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.edumind.common.api.ApiResult;
import com.edumind.common.api.PageResult;
import com.edumind.common.model.UserContext;
import com.edumind.notification.dto.broadcast.BroadcastCreateDTO;
import com.edumind.notification.service.broadcast.NotificationBroadcastService;
import com.edumind.notification.vo.broadcast.BroadcastEstimateVO;
import com.edumind.notification.vo.broadcast.BroadcastStatsVO;
import com.edumind.notification.vo.broadcast.NotificationBroadcastVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications/broadcast")
@RequiredArgsConstructor
public class NotificationBroadcastController {

    private final NotificationBroadcastService broadcastService;

    @SaCheckLogin
    @SaCheckPermission("notice:broadcast:view")
    @GetMapping("/list")
    public ApiResult<PageResult<NotificationBroadcastVO>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String targetType) {
        return ApiResult.success(broadcastService.pageList(page, pageSize, targetType));
    }

    @SaCheckLogin
    @SaCheckPermission("notice:broadcast:view")
    @GetMapping("/stats")
    public ApiResult<BroadcastStatsVO> stats() {
        return ApiResult.success(broadcastService.getStats());
    }

    @SaCheckLogin
    @SaCheckPermission("notice:broadcast:view")
    @GetMapping("/estimate")
    public ApiResult<BroadcastEstimateVO> estimate(
            @RequestParam String targetType,
            @RequestParam(required = false) String targetPayload) {
        return ApiResult.success(broadcastService.estimateAudience(targetType, targetPayload));
    }

    @SaCheckLogin
    @SaCheckPermission("notice:broadcast:send")
    @PostMapping
    public ApiResult<NotificationBroadcastVO> create(@Valid @RequestBody BroadcastCreateDTO dto) {
        Long senderId = UserContext.getUserId();
        String senderName = UserContext.get() != null ? UserContext.get().getUsername() : "";
        return ApiResult.success(broadcastService.createBroadcast(dto, senderId, senderName));
    }

    @SaCheckLogin
    @SaCheckPermission("notice:broadcast:view")
    @GetMapping("/{id}")
    public ApiResult<NotificationBroadcastVO> detail(@PathVariable Long id) {
        return ApiResult.success(broadcastService.getDetail(id));
    }

    @SaCheckLogin
    @SaCheckPermission("notice:broadcast:send")
    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        broadcastService.deleteById(id);
        return ApiResult.success();
    }

    @SaCheckLogin
    @SaCheckPermission("notice:broadcast:send")
    @DeleteMapping("/clear-all")
    public ApiResult<Void> clearAll() {
        broadcastService.clearAll();
        return ApiResult.success();
    }
}
