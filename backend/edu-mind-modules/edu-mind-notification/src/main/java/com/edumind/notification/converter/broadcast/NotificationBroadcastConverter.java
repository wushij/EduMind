package com.edumind.notification.converter.broadcast;

import com.edumind.notification.entity.NotificationBroadcastEntity;
import com.edumind.notification.vo.broadcast.NotificationBroadcastVO;
import org.springframework.beans.BeanUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class NotificationBroadcastConverter {

    public static NotificationBroadcastVO toVO(NotificationBroadcastEntity entity) {
        if (entity == null) {
            return null;
        }
        NotificationBroadcastVO vo = new NotificationBroadcastVO();
        BeanUtils.copyProperties(entity, vo);
        if (vo.getSenderName() == null || vo.getSenderName().trim().isEmpty()) {
            vo.setSenderName("admin");
        }
        return vo;
    }

    public static List<NotificationBroadcastVO> toVOList(List<NotificationBroadcastEntity> list) {
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        return list.stream().map(NotificationBroadcastConverter::toVO).collect(Collectors.toList());
    }
}
