package com.edumind.notification.service.notification;

import com.edumind.notification.vo.notification.NotificationVO;

import java.util.List;

public interface NotificationService {

    List<NotificationVO> listUnread(Long userId);

    List<NotificationVO> listAll(Long userId);

    void markAsRead(Long id, Long userId);

    void markAllAsRead(Long userId);

    long countUnread(Long userId);
}
