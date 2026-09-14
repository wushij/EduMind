package com.edumind.statistics.service.intervention.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.edumind.common.api.ResultCode;
import com.edumind.common.context.TenantContext;
import com.edumind.common.exception.BusinessException;
import com.edumind.course.api.CourseQueryApi;
import com.edumind.notification.api.NotificationWriteApi;
import com.edumind.security.context.LoginUserResolver;
import com.edumind.statistics.dao.intervention.TeachingInterventionDao;
import com.edumind.statistics.dto.intervention.InterventionActionDTO;
import com.edumind.statistics.dto.intervention.InterventionCreateDTO;
import com.edumind.statistics.entity.intervention.TeachingInterventionEntity;
import com.edumind.statistics.service.intervention.TeachingInterventionService;
import com.edumind.statistics.vo.intervention.TeachingInterventionVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 教学干预建议与决策业务服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TeachingInterventionServiceImpl implements TeachingInterventionService {

    private final TeachingInterventionDao teachingInterventionDao;
    private final CourseQueryApi courseQueryApi;
    private final NotificationWriteApi notificationWriteApi;

    @Override
    public List<TeachingInterventionVO> listInterventions(Long courseId) {
        Long tenantId = TenantContext.requireTenantId();
        List<TeachingInterventionEntity> entities = teachingInterventionDao.listByTenantAndCourse(tenantId, courseId);
        return entities.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TeachingInterventionVO createIntervention(InterventionCreateDTO dto) {
        if (dto == null || !StringUtils.hasText(dto.getTitle()) || !StringUtils.hasText(dto.getProposalText())) {
            throw new BusinessException(ResultCode.VALIDATE_FAILED.getCode(), "干预标题与方案描述不能为空");
        }

        Long tenantId = TenantContext.requireTenantId();

        TeachingInterventionEntity entity = new TeachingInterventionEntity();
        entity.setTenantId(tenantId);
        entity.setCourseId(dto.getCourseId());
        entity.setTriggerType(StringUtils.hasText(dto.getTriggerType()) ? dto.getTriggerType() : "EXAM_WEAK");
        entity.setStatus("PENDING");
        entity.setCreateTime(LocalDateTime.now());

        JSONObject json = new JSONObject();
        json.put("title", dto.getTitle());
        json.put("proposalText", dto.getProposalText());
        json.put("affectedStudentCount", dto.getAffectedStudentCount() != null ? dto.getAffectedStudentCount() : 1);
        json.put("courseName", dto.getCourseName());
        if (dto.getCustomQuestionIds() != null && !dto.getCustomQuestionIds().isEmpty()) {
            json.put("customQuestionIds", dto.getCustomQuestionIds());
        }
        entity.setProposalJson(json.toJSONString());

        teachingInterventionDao.insert(entity);
        log.info("[教学干预提案创建] 租户: {}, 干预ID: {}, 课程ID: {}, 标题: {}",
                tenantId, entity.getId(), entity.getCourseId(), dto.getTitle());

        return toVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveIntervention(Long id, InterventionActionDTO dto) {
        TeachingInterventionEntity entity = requireAccessibleIntervention(id);

        if (!"PENDING".equalsIgnoreCase(entity.getStatus())) {
            throw new BusinessException(ResultCode.VALIDATE_FAILED.getCode(), "当前干预记录状态不是待审核状态，无法重复审批");
        }

        Long userId = LoginUserResolver.resolveUserId();
        if (userId == null) {
            userId = 1L;
        }

        entity.setStatus("APPROVED");
        entity.setApprovedBy(userId);

        if (dto != null) {
            try {
                JSONObject json = StringUtils.hasText(entity.getProposalJson())
                        ? JSON.parseObject(entity.getProposalJson())
                        : new JSONObject();
                if (dto.getCustomQuestionIds() != null && !dto.getCustomQuestionIds().isEmpty()) {
                    json.put("customQuestionIds", dto.getCustomQuestionIds());
                }
                if (dto.getRemark() != null) {
                    json.put("remark", dto.getRemark());
                }
                entity.setProposalJson(json.toJSONString());
            } catch (Exception ignored) {
            }
        }

        teachingInterventionDao.updateById(entity);
        log.info("[教学干预审批] 租户: {}, 教师: {}, 干预ID: {}, 审批通过", entity.getTenantId(), userId, id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectIntervention(Long id) {
        TeachingInterventionEntity entity = requireAccessibleIntervention(id);

        if ("REVOKED".equalsIgnoreCase(entity.getStatus())) {
            throw new BusinessException(ResultCode.VALIDATE_FAILED.getCode(), "当前干预记录已被撤销");
        }

        entity.setStatus("REVOKED");
        teachingInterventionDao.updateById(entity);
        log.info("[教学干预驳回] 租户: {}, 干预ID: {}, 已驳回撤销", entity.getTenantId(), id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void dispatchIntervention(Long id) {
        TeachingInterventionEntity entity = requireAccessibleIntervention(id);

        if (!"APPROVED".equalsIgnoreCase(entity.getStatus())) {
            throw new BusinessException(ResultCode.VALIDATE_FAILED.getCode(), "只有已通过审核的干预方案方可执行下发推送");
        }

        entity.setStatus("DISPATCHED");
        teachingInterventionDao.updateById(entity);

        // 获取目标受众学生
        Long courseId = entity.getCourseId();
        List<Long> studentUserIds = null;
        if (courseId != null) {
            try {
                studentUserIds = courseQueryApi.listStudentUserIdsByCourseId(courseId);
            } catch (Exception e) {
                log.warn("[教学干预分发] 获取课程学生列表失败: {}", e.getMessage());
            }
        }

        Long currentUserId = LoginUserResolver.resolveUserId();
        List<Long> finalUserIds = (studentUserIds != null && !studentUserIds.isEmpty())
                ? studentUserIds
                : (currentUserId != null ? List.of(currentUserId) : List.of(1L));

        // 提取标题与内容
        String title = "教学干预推送";
        String content = "您有新的教学针对性干预方案，请及时查收并完成学习任务。";
        if (StringUtils.hasText(entity.getProposalJson())) {
            try {
                JSONObject json = JSON.parseObject(entity.getProposalJson());
                if (json.containsKey("title") && json.getString("title") != null) {
                    title = "【干预推送】" + json.getString("title");
                }
                if (json.containsKey("proposalText") && json.getString("proposalText") != null) {
                    content = json.getString("proposalText");
                }
            } catch (Exception ignored) {
            }
        }

        // 调用 NotificationWriteApi 写入 sys_notification 并下发
        notificationWriteApi.sendToUsers(
                entity.getTenantId(),
                finalUserIds,
                title,
                content,
                "INTERVENTION",
                entity.getId()
        );

        log.info("[教学干预分发] 租户: {}, 干预ID: {}, 成功向 {} 名学生推送通知",
                entity.getTenantId(), id, finalUserIds.size());
    }

    private TeachingInterventionEntity requireAccessibleIntervention(Long id) {
        TeachingInterventionEntity entity = teachingInterventionDao.findByIdIgnoreTenant(id);
        if (entity == null) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND.getCode(), "教学干预记录不存在");
        }

        // 租户隔离校验 (IDOR 越权拦截)
        Long currentTenantId = TenantContext.getTenantId();
        if (currentTenantId != null && entity.getTenantId() != null && !currentTenantId.equals(entity.getTenantId())) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "无权操作其他租户的教学干预建议 (IDOR 越权拦截)");
        }

        return entity;
    }

    private TeachingInterventionVO toVO(TeachingInterventionEntity entity) {
        TeachingInterventionVO vo = new TeachingInterventionVO();
        vo.setId(entity.getId());
        vo.setTenantId(entity.getTenantId());
        vo.setCourseId(entity.getCourseId());
        vo.setTriggerType(entity.getTriggerType());
        vo.setStatus(entity.getStatus());
        vo.setApprovedBy(entity.getApprovedBy() != null ? "骨干教师(ID:" + entity.getApprovedBy() + ")" : "-");
        vo.setCreateTime(entity.getCreateTime());

        if (entity.getProposalJson() != null && !entity.getProposalJson().isBlank()) {
            try {
                JSONObject json = JSON.parseObject(entity.getProposalJson());
                vo.setTitle(json.getString("title"));
                vo.setProposalText(json.getString("proposalText"));
                vo.setAffectedStudentCount(json.getInteger("affectedStudentCount"));
                vo.setCourseName(json.getString("courseName"));
            } catch (Exception e) {
                vo.setTitle("智能学情干预建议");
                vo.setProposalText(entity.getProposalJson());
            }
        }
        return vo;
    }
}
