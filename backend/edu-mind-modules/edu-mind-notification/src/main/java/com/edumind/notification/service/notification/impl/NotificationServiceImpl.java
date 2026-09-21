package com.edumind.notification.service.notification.impl;

import com.edumind.common.api.PageResult;
import com.edumind.common.exception.BusinessException;
import com.edumind.notification.converter.notification.NotificationConverter;
import com.edumind.notification.dao.NotificationBroadcastDao;
import com.edumind.notification.dao.NotificationDao;
import com.edumind.notification.entity.NotificationEntity;
import com.edumind.notification.service.notification.NotificationService;
import com.edumind.notification.vo.notification.NotificationListVO;
import com.edumind.notification.vo.notification.NotificationUnreadCountVO;
import com.edumind.notification.vo.notification.NotificationVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationDao notificationDao;
    private final NotificationBroadcastDao broadcastDao;

    @Override
    public List<NotificationVO> listUnread(Long userId) {
        List<NotificationEntity> entities = notificationDao.findUnreadByUserId(userId);
        return NotificationConverter.toVOList(entities);
    }

    @Override
    public List<NotificationVO> listAll(Long userId) {
        List<NotificationEntity> entities = notificationDao.findByUserId(userId);
        return NotificationConverter.toVOList(entities);
    }

    @Override
    public NotificationListVO pageList(Long userId, String category, long page, long pageSize) {
        PageResult<NotificationEntity> pageResult = notificationDao.pageByUserId(userId, category, page, pageSize);
        NotificationListVO result = new NotificationListVO();
        result.setUnreadCount(notificationDao.countUnreadByUserId(userId));
        result.setTotalCount(pageResult.getTotal());
        PageResult<NotificationVO> voPage = PageResult.<NotificationVO>builder()
                .total(pageResult.getTotal())
                .pageNum(pageResult.getPageNum())
                .pageSize(pageResult.getPageSize())
                .list(NotificationConverter.toVOList(pageResult.getList()))
                .build();
        result.setList(voPage);
        return result;
    }

    @Override
    public NotificationUnreadCountVO getUnreadCount(Long userId) {
        NotificationUnreadCountVO vo = new NotificationUnreadCountVO();
        vo.setUnreadCount(notificationDao.countUnreadByUserId(userId));
        vo.setTotalCount(notificationDao.countByUserId(userId));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAsRead(Long id, Long userId) {
        NotificationEntity entity = notificationDao.findById(id);
        if (entity == null || !entity.getUserId().equals(userId)) {
            throw new BusinessException("通知不存在或无权操作");
        }
        if (entity.getIsRead() != null && entity.getIsRead() == 1) {
            return;
        }
        int updated = notificationDao.markAsRead(id, userId);
        if (updated == 0) {
            throw new BusinessException("通知不存在或无权操作");
        }
        if ("BROADCAST".equals(entity.getType()) && entity.getRefId() != null) {
            broadcastDao.incrementReadCount(entity.getRefId());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAllAsRead(Long userId) {
        List<Long> unreadBroadcastRefIds = notificationDao.findUnreadBroadcastRefIdsByUserId(userId);
        notificationDao.markAllAsReadByUserId(userId);
        if (unreadBroadcastRefIds != null && !unreadBroadcastRefIds.isEmpty()) {
            // 单条批量 UPDATE 替代循环内逐条递增
            broadcastDao.incrementReadCountBatch(unreadBroadcastRefIds);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteById(Long id, Long userId) {
        int deleted = notificationDao.deleteById(id, userId);
        if (deleted == 0) {
            throw new BusinessException("通知不存在或无权操作");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clearAll(Long userId) {
        notificationDao.deleteAllByUserId(userId);
    }

    @Override
    public long countUnread(Long userId) {
        return notificationDao.countUnreadByUserId(userId);
    }
}
