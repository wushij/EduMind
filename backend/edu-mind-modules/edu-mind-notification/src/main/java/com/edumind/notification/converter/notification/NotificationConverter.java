package com.edumind.notification.converter.notification;

import com.edumind.notification.entity.NotificationEntity;
import com.edumind.notification.vo.notification.NotificationVO;
import org.springframework.beans.BeanUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class NotificationConverter {

    public static NotificationVO toVO(NotificationEntity entity) {
        if (entity == null) {
            return null;
        }
        NotificationVO vo = new NotificationVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }

    public static List<NotificationVO> toVOList(List<NotificationEntity> list) {
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        return list.stream().map(NotificationConverter::toVO).collect(Collectors.toList());
    }
}
