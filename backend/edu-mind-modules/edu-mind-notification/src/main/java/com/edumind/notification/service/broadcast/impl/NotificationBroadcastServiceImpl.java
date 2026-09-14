package com.edumind.notification.service.broadcast.impl;

import com.edumind.common.api.PageResult;
import com.edumind.common.api.ResultCode;
import com.edumind.common.context.TenantContext;
import com.edumind.common.exception.BusinessException;
import com.edumind.notification.converter.broadcast.NotificationBroadcastConverter;
import com.edumind.notification.dao.NotificationBroadcastDao;
import com.edumind.notification.dao.NotificationDao;
import com.edumind.notification.dto.broadcast.BroadcastCreateDTO;
import com.edumind.notification.entity.NotificationBroadcastEntity;
import com.edumind.notification.service.broadcast.NotificationBroadcastDispatcher;
import com.edumind.notification.service.broadcast.NotificationBroadcastService;
import com.edumind.notification.vo.broadcast.BroadcastEstimateVO;
import com.edumind.notification.vo.broadcast.BroadcastStatsVO;
import com.edumind.notification.vo.broadcast.NotificationBroadcastVO;
import com.edumind.system.api.UserQueryApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class NotificationBroadcastServiceImpl implements NotificationBroadcastService {

    private static final Set<String> ROLE_CODES = Set.of("ADMIN", "TEACHER", "STUDENT");

    private final NotificationBroadcastDao broadcastDao;
    private final NotificationDao notificationDao;
    private final NotificationBroadcastDispatcher broadcastDispatcher;
    private final UserQueryApi userQueryApi;

    @Override
    public BroadcastEstimateVO estimateAudience(String targetType, String targetPayload) {
        Long tenantId = TenantContext.requireTenantId();
        long count = resolveAudienceCount(tenantId, targetType, targetPayload);
        BroadcastEstimateVO vo = new BroadcastEstimateVO();
        vo.setEstimatedCount(count);
        vo.setTargetType(targetType);
        vo.setFormattedDesc(buildAudienceDesc(targetType, targetPayload, count));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NotificationBroadcastVO createBroadcast(BroadcastCreateDTO dto, Long senderId, String senderName) {
        Long tenantId = TenantContext.requireTenantId();
        String title = dto.getTitle() != null ? dto.getTitle().trim() : "";
        String content = dto.getContent() != null ? dto.getContent().trim() : "";
        if (!StringUtils.hasText(title) || !StringUtils.hasText(content)) {
            throw new BusinessException("推送标题与内容不能为空");
        }
        String targetType = normalizeTargetType(dto.getTargetType());
        String targetPayload = normalizeTargetPayload(targetType, dto.getTargetPayload());
        int priority = dto.getPriority() != null ? dto.getPriority() : 0;

        List<Long> userIds = resolveAudienceUserIds(tenantId, targetType, targetPayload);
        if (userIds.isEmpty()) {
            throw new BusinessException("当前受众条件下无目标用户");
        }

        NotificationBroadcastEntity broadcast = new NotificationBroadcastEntity();
        broadcast.setTenantId(tenantId);
        broadcast.setTitle(title);
        broadcast.setContent(content);
        broadcast.setTargetType(targetType);
        broadcast.setTargetPayload(targetPayload);
        broadcast.setNotifyType("BROADCAST");
        broadcast.setPriority(priority);
        broadcast.setSenderId(senderId);
        broadcast.setSenderName(senderName != null ? senderName : "");
        broadcast.setTotalCount(userIds.size());
        broadcast.setReadCount(0);
        broadcastDao.insert(broadcast);

        broadcastDispatcher.dispatch(tenantId, broadcast.getId(), userIds, title, content, priority);

        return NotificationBroadcastConverter.toVO(broadcast);
    }

    @Override
    public PageResult<NotificationBroadcastVO> pageList(long page, long pageSize, String targetType) {
        PageResult<NotificationBroadcastEntity> pageResult = broadcastDao.page(page, pageSize, targetType);
        return PageResult.<NotificationBroadcastVO>builder()
                .total(pageResult.getTotal())
                .pageNum(pageResult.getPageNum())
                .pageSize(pageResult.getPageSize())
                .list(NotificationBroadcastConverter.toVOList(pageResult.getList()))
                .build();
    }

    @Override
    public NotificationBroadcastVO getDetail(Long id) {
        NotificationBroadcastEntity entity = broadcastDao.findById(id);
        if (entity == null) {
            throw new BusinessException("广播记录不存在");
        }
        Long currentTenantId = TenantContext.getTenantId();
        if (currentTenantId != null && entity.getTenantId() != null && !currentTenantId.equals(entity.getTenantId())) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "无权访问其他租户广播记录");
        }
        return NotificationBroadcastConverter.toVO(entity);
    }

    @Override
    public BroadcastStatsVO getStats() {
        BroadcastStatsVO vo = new BroadcastStatsVO();
        vo.setTotalBroadcasts(broadcastDao.countAll());
        long reach = broadcastDao.sumTotalReach();
        long read = broadcastDao.sumReadCount();
        vo.setTotalReach(reach);
        vo.setTotalRead(read);
        vo.setAvgReadRate(reach > 0 ? (read * 100.0 / reach) : 0.0);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteById(Long id) {
        NotificationBroadcastEntity entity = broadcastDao.findById(id);
        if (entity == null) {
            throw new BusinessException("广播记录不存在");
        }
        Long currentTenantId = TenantContext.getTenantId();
        if (currentTenantId != null && entity.getTenantId() != null && !currentTenantId.equals(entity.getTenantId())) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "无权删除其他租户广播记录");
        }
        broadcastDao.deleteById(id);
        notificationDao.deleteByRefId(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clearAll() {
        Long currentTenantId = TenantContext.requireTenantId();
        broadcastDao.deleteAllByTenantId(currentTenantId);
        notificationDao.deleteAllByTypeAndTenantId("BROADCAST", currentTenantId);
    }

    private String normalizeTargetType(String targetType) {
        if (!StringUtils.hasText(targetType)) {
            return "all";
        }
        return targetType.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeTargetPayload(String targetType, String targetPayload) {
        if (!"role".equals(targetType)) {
            return null;
        }
        if (!StringUtils.hasText(targetPayload)) {
            throw new BusinessException("请选择推送角色");
        }
        String role = targetPayload.trim().toUpperCase(Locale.ROOT);
        if (!ROLE_CODES.contains(role)) {
            throw new BusinessException("不支持的角色类型: " + role);
        }
        return role;
    }

    private List<Long> resolveAudienceUserIds(Long tenantId, String targetType, String targetPayload) {
        if ("all".equals(targetType)) {
            return new ArrayList<>(userQueryApi.listActiveUserIdsByTenantId(tenantId));
        }
        if ("role".equals(targetType)) {
            return new ArrayList<>(userQueryApi.listUserIdsByTenantAndRole(tenantId, targetPayload));
        }
        throw new BusinessException("不支持的推送对象类型");
    }

    private long resolveAudienceCount(Long tenantId, String targetType, String targetPayload) {
        String normalized = normalizeTargetType(targetType);
        if ("all".equals(normalized)) {
            return userQueryApi.countActiveUsersByTenantId(tenantId);
        }
        if ("role".equals(normalized)) {
            String role = normalizeTargetPayload("role", targetPayload);
            return userQueryApi.countUsersByTenantAndRole(tenantId, role);
        }
        return 0;
    }

    private String buildAudienceDesc(String targetType, String targetPayload, long count) {
        String normalized = normalizeTargetType(targetType);
        if ("all".equals(normalized)) {
            return String.format("本校预计触达全体成员 %d 人", count);
        }
        if ("role".equals(normalized)) {
            String role = targetPayload != null ? targetPayload.toUpperCase(Locale.ROOT) : "";
            String roleName = switch (role) {
                case "ADMIN" -> "系统管理员";
                case "TEACHER" -> "教师";
                case "STUDENT" -> "学生";
                default -> role;
            };
            return String.format("本校预计触达【%s】 %d 人", roleName, count);
        }
        return "本校预计触达 0 人";
    }
}

