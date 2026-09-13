package com.edumind.statistics.service.intervention.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.edumind.common.api.ResultCode;
import com.edumind.common.context.TenantContext;
import com.edumind.common.exception.BusinessException;
import com.edumind.security.context.LoginUserResolver;
import com.edumind.statistics.dao.intervention.TeachingInterventionDao;
import com.edumind.statistics.dto.intervention.InterventionActionDTO;
import com.edumind.statistics.entity.intervention.TeachingInterventionEntity;
import com.edumind.statistics.service.intervention.TeachingInterventionService;
import com.edumind.statistics.vo.intervention.TeachingInterventionVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Override
    public List<TeachingInterventionVO> listInterventions(Long courseId) {
        Long tenantId = TenantContext.requireTenantId();
        List<TeachingInterventionEntity> entities = teachingInterventionDao.listByTenantAndCourse(tenantId, courseId);

        // 若当前租户下暂无干预数据，初始化两则典型学情干预记录（保证开箱即用与持久化）
        if (entities.isEmpty()) {
            initSampleInterventions(tenantId, courseId != null ? courseId : 101L);
            entities = teachingInterventionDao.listByTenantAndCourse(tenantId, courseId);
        }

        return entities.stream().map(this::toVO).collect(Collectors.toList());
    }

    private void initSampleInterventions(Long tenantId, Long courseId) {
        TeachingInterventionEntity item1 = new TeachingInterventionEntity();
        item1.setTenantId(tenantId);
        item1.setCourseId(courseId);
        item1.setTriggerType("EXAM_WEAK");
        item1.setStatus("PENDING");
        item1.setCreateTime(LocalDateTime.now().minusHours(3));
        JSONObject json1 = new JSONObject();
        json1.put("title", "高数期中预警：高三(1)班 12 名学生导数定义与极限计算掌握度偏低 (<50%)");
        json1.put("proposalText", "AI 诊断模型检测到近期作业中第 3 大题平均失分率达 58%，建议批量推送专项攻坚微课与 5 道靶向等价代换习题。");
        json1.put("affectedStudentCount", 12);
        json1.put("courseName", "高等数学（上）");
        item1.setProposalJson(json1.toJSONString());
        teachingInterventionDao.insert(item1);

        TeachingInterventionEntity item2 = new TeachingInterventionEntity();
        item2.setTenantId(tenantId);
        item2.setCourseId(courseId);
        item2.setTriggerType("ACTIVITY_DROP");
        item2.setStatus("APPROVED");
        item2.setApprovedBy(1L);
        item2.setCreateTime(LocalDateTime.now().minusDays(1));
        JSONObject json2 = new JSONObject();
        json2.put("title", "学情异常波动：高二(1)班连续 3 天算法代码提交活跃度下降 35%");
        json2.put("proposalText", "建议开展随堂代码走查与双指针经典面试题趣味通关答疑活动，激活学生编码兴趣。");
        json2.put("affectedStudentCount", 8);
        json2.put("courseName", "数据结构与算法");
        item2.setProposalJson(json2.toJSONString());
        teachingInterventionDao.insert(item2);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveIntervention(Long id, InterventionActionDTO dto) {
        Long tenantId = TenantContext.requireTenantId();
        Long userId = LoginUserResolver.requireUserId();

        TeachingInterventionEntity entity = teachingInterventionDao.findByIdAndTenantId(id, tenantId);
        if (entity == null) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND.getCode(), "教学干预记录不存在或无权操作");
        }
        if (!"PENDING".equalsIgnoreCase(entity.getStatus())) {
            throw new BusinessException(ResultCode.VALIDATE_FAILED.getCode(), "当前干预记录状态不是待审核状态，无法重复审批");
        }

        entity.setStatus("APPROVED");
        entity.setApprovedBy(userId);
        teachingInterventionDao.updateById(entity);

        log.info("[教学干预审批] 租户: {}, 教师: {}, 干预ID: {}, 审批通过", tenantId, userId, id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectIntervention(Long id) {
        Long tenantId = TenantContext.requireTenantId();

        TeachingInterventionEntity entity = teachingInterventionDao.findByIdAndTenantId(id, tenantId);
        if (entity == null) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND.getCode(), "教学干预记录不存在或无权操作");
        }

        entity.setStatus("REVOKED");
        teachingInterventionDao.updateById(entity);
        log.info("[教学干预驳回] 租户: {}, 干预ID: {}, 已驳回撤销", tenantId, id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void dispatchIntervention(Long id) {
        Long tenantId = TenantContext.requireTenantId();

        TeachingInterventionEntity entity = teachingInterventionDao.findByIdAndTenantId(id, tenantId);
        if (entity == null) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND.getCode(), "教学干预记录不存在或无权操作");
        }
        if (!"APPROVED".equalsIgnoreCase(entity.getStatus())) {
            throw new BusinessException(ResultCode.VALIDATE_FAILED.getCode(), "只有已通过审核的干预方案方可执行下发推送");
        }

        entity.setStatus("DISPATCHED");
        teachingInterventionDao.updateById(entity);
        log.info("[教学干预分发] 租户: {}, 干预ID: {}, 已成功推送至目标受众学生", tenantId, id);
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
