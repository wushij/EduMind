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
import com.edumind.notification.entity.NotificationEntity;
import com.edumind.notification.service.broadcast.NotificationBroadcastDispatcher;
import com.edumind.notification.service.broadcast.NotificationBroadcastService;
import com.edumind.notification.vo.broadcast.BroadcastEstimateVO;
import com.edumind.notification.vo.broadcast.BroadcastStatsVO;
import com.edumind.notification.vo.broadcast.NotificationBroadcastVO;
import com.edumind.system.api.UserQueryApi;
import com.edumind.system.vo.user.UserBriefVO;
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

        if (!StringUtils.hasText(senderName) && senderId != null) {
            try {
                UserBriefVO u = userQueryApi.getUserById(senderId);
                if (u != null && StringUtils.hasText(u.getUsername())) {
                    senderName = u.getUsername();
                }
            } catch (Exception ignored) {
            }
        }
        if (!StringUtils.hasText(senderName)) {
            senderName = "admin";
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
        broadcast.setSenderName(senderName);
        broadcast.setTotalCount(userIds.size());
        broadcast.setReadCount(0);
        broadcastDao.insert(broadcast);

        broadcastDispatcher.dispatch(tenantId, broadcast.getId(), userIds, title, content, priority);

        return NotificationBroadcastConverter.toVO(broadcast);
    }

    @Override
    public PageResult<NotificationBroadcastVO> pageList(long page, long pageSize, String targetType) {
        PageResult<NotificationBroadcastEntity> pageResult = broadcastDao.page(page, pageSize, targetType);
        List<NotificationBroadcastVO> voList = NotificationBroadcastConverter.toVOList(pageResult.getList());
        for (NotificationBroadcastVO vo : voList) {
            populateSenderInfo(vo);
        }
        return PageResult.<NotificationBroadcastVO>builder()
                .total(pageResult.getTotal())
                .pageNum(pageResult.getPageNum())
                .pageSize(pageResult.getPageSize())
                .list(voList)
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
        NotificationBroadcastVO vo = NotificationBroadcastConverter.toVO(entity);
        populateSenderInfo(vo);
        return vo;
    }

    private void populateSenderInfo(NotificationBroadcastVO vo) {
        if (vo == null || vo.getSenderId() == null) {
            return;
        }
        try {
            UserBriefVO u = userQueryApi.getUserById(vo.getSenderId());
            if (u != null) {
                if (StringUtils.hasText(u.getAvatar())) {
                    vo.setSenderAvatar(u.getAvatar());
                }
                if (!StringUtils.hasText(vo.getSenderName()) && StringUtils.hasText(u.getUsername())) {
                    vo.setSenderName(u.getUsername());
                }
            }
        } catch (Exception ignored) {
        }
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
    public com.edumind.notification.vo.broadcast.BroadcastRecipientSummaryVO getRecipientSummary(
            Long broadcastId, Integer isRead, String keyword, long page, long pageSize) {
        NotificationBroadcastEntity broadcast = broadcastDao.findById(broadcastId);
        if (broadcast == null) {
            throw new BusinessException("广播记录不存在");
        }
        Long currentTenantId = TenantContext.getTenantId();
        if (currentTenantId != null && broadcast.getTenantId() != null && !currentTenantId.equals(broadcast.getTenantId())) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "无权访问其他租户广播明细");
        }

        long total = notificationDao.countByBroadcast(broadcastId);
        long read = notificationDao.countReadByBroadcast(broadcastId);
        long unread = Math.max(0, total - read);
        int rate = total > 0 ? (int) Math.min(Math.round((read * 100.0) / total), 100) : 0;

        List<Long> matchedUserIds = null;
        if (StringUtils.hasText(keyword)) {
            matchedUserIds = userQueryApi.findUserIdsByKeyword(keyword.trim());
        }

        PageResult<NotificationEntity> entityPage = notificationDao.pageByBroadcast(broadcastId, isRead, matchedUserIds, page, pageSize);

        List<com.edumind.notification.vo.broadcast.BroadcastRecipientVO> recipientVOList = new ArrayList<>();
        for (NotificationEntity entity : entityPage.getList()) {
            com.edumind.notification.vo.broadcast.BroadcastRecipientVO rvo = new com.edumind.notification.vo.broadcast.BroadcastRecipientVO();
            rvo.setId(entity.getId());
            rvo.setUserId(entity.getUserId());
            rvo.setIsRead(entity.getIsRead() != null ? entity.getIsRead() : 0);
            rvo.setCreateTime(entity.getCreateTime());

            if (entity.getUserId() != null) {
                try {
                    UserBriefVO u = userQueryApi.getUserById(entity.getUserId());
                    if (u != null) {
                        rvo.setUsername(u.getUsername());
                        rvo.setRealName(u.getRealName());
                        rvo.setAvatar(u.getAvatar());
                    }
                } catch (Exception ignored) {}

                try {
                    List<String> roles = userQueryApi.getRolesByUserId(entity.getUserId());
                    if (roles != null && !roles.isEmpty()) {
                        String primaryRole = roles.get(0);
                        rvo.setRoleCode(primaryRole);
                        rvo.setRoleName(switch (primaryRole.toUpperCase()) {
                            case "ADMIN" -> "系统管理员";
                            case "TEACHER" -> "教师";
                            case "STUDENT" -> "学生";
                            default -> primaryRole;
                        });
                    } else {
                        rvo.setRoleCode("USER");
                        rvo.setRoleName("普通用户");
                    }
                } catch (Exception ignored) {}
            }
            if (!StringUtils.hasText(rvo.getUsername())) {
                rvo.setUsername("用户#" + entity.getUserId());
            }
            recipientVOList.add(rvo);
        }

        PageResult<com.edumind.notification.vo.broadcast.BroadcastRecipientVO> recipientPageResult =
                PageResult.<com.edumind.notification.vo.broadcast.BroadcastRecipientVO>builder()
                        .total(entityPage.getTotal())
                        .pageNum(entityPage.getPageNum())
                        .pageSize(entityPage.getPageSize())
                        .list(recipientVOList)
                        .build();

        com.edumind.notification.vo.broadcast.BroadcastRecipientSummaryVO summaryVO =
                new com.edumind.notification.vo.broadcast.BroadcastRecipientSummaryVO();
        summaryVO.setBroadcastId(broadcastId);
        summaryVO.setBroadcastTitle(broadcast.getTitle());
        summaryVO.setTotalCount((int) total);
        summaryVO.setReadCount((int) read);
        summaryVO.setUnreadCount((int) unread);
        summaryVO.setReadRate(rate);
        summaryVO.setRecipients(recipientPageResult);
        return summaryVO;
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

