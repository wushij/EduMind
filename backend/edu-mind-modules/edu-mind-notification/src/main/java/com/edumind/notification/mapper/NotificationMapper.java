package com.edumind.notification.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.edumind.notification.entity.NotificationEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 消息通知 Mapper
 */
@Mapper
public interface NotificationMapper extends BaseMapper<NotificationEntity> {
}
