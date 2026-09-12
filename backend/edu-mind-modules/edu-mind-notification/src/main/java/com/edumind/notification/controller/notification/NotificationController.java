package com.edumind.notification.controller.notification;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.edumind.common.api.ApiResult;
import com.edumind.common.model.UserContext;
import com.edumind.notification.service.notification.NotificationService;
import com.edumind.notification.vo.notification.NotificationVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @SaCheckLogin
    @GetMapping
    public ApiResult<List<NotificationVO>> listUnread() {
        Long userId = UserContext.getUserId();
        return ApiResult.success(notificationService.listUnread(userId));
    }

    @SaCheckLogin
    @GetMapping("/all")
    public ApiResult<List<NotificationVO>> listAll() {
        Long userId = UserContext.getUserId();
        return ApiResult.success(notificationService.listAll(userId));
    }

    @SaCheckLogin
    @PutMapping("/{id}/read")
    public ApiResult<Void> markAsRead(@PathVariable("id") Long id) {
        Long userId = UserContext.getUserId();
        notificationService.markAsRead(id, userId);
        return ApiResult.success();
    }

    @SaCheckLogin
    @PutMapping("/read-all")
    public ApiResult<Void> markAllAsRead() {
        Long userId = UserContext.getUserId();
        notificationService.markAllAsRead(userId);
        return ApiResult.success();
    }
}
