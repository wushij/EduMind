package com.edumind.notification.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edumind.common.api.PageResult;
import com.edumind.notification.entity.NotificationBroadcastEntity;
import com.edumind.notification.mapper.NotificationBroadcastMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
@RequiredArgsConstructor
public class NotificationBroadcastDao {

    private final NotificationBroadcastMapper broadcastMapper;

    public int insert(NotificationBroadcastEntity entity) {
        return broadcastMapper.insert(entity);
    }

    public NotificationBroadcastEntity findById(Long id) {
        return broadcastMapper.selectById(id);
    }

    public int deleteById(Long id) {
        return broadcastMapper.deleteById(id);
    }

    public int deleteAll() {
        return broadcastMapper.delete(new LambdaQueryWrapper<>());
    }

    public int deleteAllByTenantId(Long tenantId) {
        if (tenantId == null) {
            return 0;
        }
        return broadcastMapper.delete(new LambdaQueryWrapper<NotificationBroadcastEntity>()
                .eq(NotificationBroadcastEntity::getTenantId, tenantId));
    }

    public int incrementReadCount(Long id) {
        NotificationBroadcastEntity entity = findById(id);
        if (entity == null) {
            return 0;
        }
        entity.setReadCount((entity.getReadCount() == null ? 0 : entity.getReadCount()) + 1);
        return broadcastMapper.updateById(entity);
    }

    public PageResult<NotificationBroadcastEntity> page(long pageNum, long pageSize, String targetType) {
        LambdaQueryWrapper<NotificationBroadcastEntity> wrapper = new LambdaQueryWrapper<NotificationBroadcastEntity>()
                .orderByDesc(NotificationBroadcastEntity::getCreateTime);
        if (StringUtils.hasText(targetType)) {
            wrapper.eq(NotificationBroadcastEntity::getTargetType, targetType);
        }
        Page<NotificationBroadcastEntity> page = broadcastMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.<NotificationBroadcastEntity>builder()
                .total(page.getTotal())
                .pageNum(page.getCurrent())
                .pageSize(page.getSize())
                .list(page.getRecords())
                .build();
    }

    public long countAll() {
        return broadcastMapper.selectCount(new LambdaQueryWrapper<>());
    }

    public long sumTotalReach() {
        return broadcastMapper.selectList(new LambdaQueryWrapper<NotificationBroadcastEntity>()
                        .select(NotificationBroadcastEntity::getTotalCount))
                .stream()
                .mapToLong(e -> e.getTotalCount() == null ? 0 : e.getTotalCount())
                .sum();
    }

    public long sumReadCount() {
        return broadcastMapper.selectList(new LambdaQueryWrapper<NotificationBroadcastEntity>()
                        .select(NotificationBroadcastEntity::getReadCount))
                .stream()
                .mapToLong(e -> e.getReadCount() == null ? 0 : e.getReadCount())
                .sum();
    }
}
