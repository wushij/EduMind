package com.edumind.notification.service.notification;

import com.edumind.notification.vo.notification.NotificationListVO;
import com.edumind.notification.vo.notification.NotificationUnreadCountVO;
import com.edumind.notification.vo.notification.NotificationVO;

import java.util.List;

public interface NotificationService {

    List<NotificationVO> listUnread(Long userId);

    List<NotificationVO> listAll(Long userId);

    NotificationListVO pageList(Long userId, String category, long page, long pageSize);

    NotificationUnreadCountVO getUnreadCount(Long userId);

    void markAsRead(Long id, Long userId);

    void markAllAsRead(Long userId);

    void deleteById(Long id, Long userId);

    void clearAll(Long userId);

    long countUnread(Long userId);
}
