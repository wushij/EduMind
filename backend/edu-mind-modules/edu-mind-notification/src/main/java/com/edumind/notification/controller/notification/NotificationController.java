package com.edumind.notification.controller.notification;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.edumind.common.api.ApiResult;
import com.edumind.common.model.UserContext;
import com.edumind.notification.dao.NotificationDao;
import com.edumind.notification.entity.NotificationEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationDao notificationDao;

    @SaCheckLogin
    @GetMapping
    public ApiResult<List<NotificationEntity>> listUnread() {
        Long userId = UserContext.getUserId();
        return ApiResult.success(notificationDao.findUnreadByUserId(userId));
    }
}
