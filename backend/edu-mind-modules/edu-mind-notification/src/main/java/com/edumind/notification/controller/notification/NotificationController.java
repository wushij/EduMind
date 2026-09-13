package com.edumind.notification.controller.notification;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.edumind.common.api.ApiResult;
import com.edumind.common.model.UserContext;
import com.edumind.notification.service.notification.NotificationService;
import com.edumind.notification.vo.notification.NotificationListVO;
import com.edumind.notification.vo.notification.NotificationUnreadCountVO;
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
    @GetMapping("/list")
    public ApiResult<NotificationListVO> pageList(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "50") long pageSize,
            @RequestParam(defaultValue = "all") String category) {
        Long userId = UserContext.getUserId();
        return ApiResult.success(notificationService.pageList(userId, category, page, pageSize));
    }

    @SaCheckLogin
    @GetMapping("/unread-count")
    public ApiResult<NotificationUnreadCountVO> unreadCount() {
        Long userId = UserContext.getUserId();
        return ApiResult.success(notificationService.getUnreadCount(userId));
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

    @SaCheckLogin
    @DeleteMapping("/{id}")
    public ApiResult<Void> deleteById(@PathVariable("id") Long id) {
        Long userId = UserContext.getUserId();
        notificationService.deleteById(id, userId);
        return ApiResult.success();
    }

    @SaCheckLogin
    @DeleteMapping("/clear-all")
    public ApiResult<Void> clearAll() {
        Long userId = UserContext.getUserId();
        notificationService.clearAll(userId);
        return ApiResult.success();
    }
}
