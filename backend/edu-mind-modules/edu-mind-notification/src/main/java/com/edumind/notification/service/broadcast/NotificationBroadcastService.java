package com.edumind.notification.service.broadcast;

import com.edumind.common.api.PageResult;
import com.edumind.notification.dto.broadcast.BroadcastCreateDTO;
import com.edumind.notification.vo.broadcast.BroadcastEstimateVO;
import com.edumind.notification.vo.broadcast.BroadcastStatsVO;
import com.edumind.notification.vo.broadcast.NotificationBroadcastVO;

public interface NotificationBroadcastService {

    BroadcastEstimateVO estimateAudience(String targetType, String targetPayload);

    NotificationBroadcastVO createBroadcast(BroadcastCreateDTO dto, Long senderId, String senderName);

    PageResult<NotificationBroadcastVO> pageList(long page, long pageSize, String targetType);

    NotificationBroadcastVO getDetail(Long id);

    BroadcastStatsVO getStats();

    void deleteById(Long id);

    void clearAll();
}
