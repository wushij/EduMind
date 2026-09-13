package com.edumind.system.service.impl;

import com.edumind.common.api.ResultCode;
import com.edumind.common.context.TenantContext;
import com.edumind.common.exception.BusinessException;
import com.edumind.system.api.OrganizationQueryApi;
import com.edumind.system.dao.SysMemberOrgDao;
import com.edumind.system.dao.SysOrganizationDao;
import com.edumind.system.dao.SysTenantMemberDao;
import com.edumind.system.dao.UserDao;
import com.edumind.system.entity.SysMemberOrgEntity;
import com.edumind.system.entity.SysOrganizationEntity;
import com.edumind.system.entity.SysTenantMemberEntity;
import com.edumind.system.entity.UserEntity;
import com.edumind.system.service.SysOrganizationService;
import com.edumind.system.vo.tenant.OrganizationMemberVO;
import com.edumind.system.vo.tenant.OrganizationNodeVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SysOrganizationServiceImpl implements SysOrganizationService, OrganizationQueryApi {

    private final SysOrganizationDao sysOrganizationDao;
    private final SysMemberOrgDao sysMemberOrgDao;
    private final SysTenantMemberDao sysTenantMemberDao;
    private final UserDao userDao;

    private Long resolveAndVerifyTenantId(Long explicitTenantId) {
        Long currentTenantId = TenantContext.requireTenantId();
        if (explicitTenantId != null && !explicitTenantId.equals(currentTenantId)) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "无权访问其他学校组织架构");
        }
        return currentTenantId;
    }

    @Override
    public List<OrganizationNodeVO> getTree(Long tenantId) {
        Long resolvedTenantId = resolveAndVerifyTenantId(tenantId);
        List<SysOrganizationEntity> entities = sysOrganizationDao.listByTenantId(resolvedTenantId);
        return buildTree(entities, resolvedTenantId);
    }

    @Override
    public List<OrganizationMemberVO> getOrgMembers(Long tenantId, Long orgId) {
        Long resolvedTenantId = resolveAndVerifyTenantId(tenantId);
        SysOrganizationEntity org = sysOrganizationDao.findByIdAndTenantId(orgId, resolvedTenantId);
        if (org == null) {
            return Collections.emptyList();
        }

        List<SysMemberOrgEntity> relations = sysMemberOrgDao.listByOrgId(resolvedTenantId, orgId);
        if (CollectionUtils.isEmpty(relations)) {
            return Collections.emptyList();
        }

        Map<Long, String> roleTypeByMemberId = relations.stream()
                .collect(Collectors.toMap(
                        SysMemberOrgEntity::getMemberId,
                        SysMemberOrgEntity::getRoleType,
                        (left, right) -> left));

        List<Long> memberIds = relations.stream()
                .map(SysMemberOrgEntity::getMemberId)
                .distinct()
                .collect(Collectors.toList());

        List<SysTenantMemberEntity> members = sysTenantMemberDao.listByIds(tenantId, memberIds);
        return members.stream().map(member -> {
            UserEntity user = userDao.findById(member.getUserId());
            String displayName = member.getRealName() != null ? member.getRealName() : "学员 " + member.getUserId();
            String avatar = user != null && user.getAvatar() != null && !user.getAvatar().isBlank()
                    ? user.getAvatar()
                    : "https://api.dicebear.com/7.x/avataaars/svg?seed=" + displayName;
            return OrganizationMemberVO.builder()
                    .id(member.getId())
                    .userId(member.getUserId())
                    .studentNo(member.getMemberNo() != null ? member.getMemberNo() : "STU-" + member.getUserId())
                    .name(displayName)
                    .role(resolveOrgRoleLabel(roleTypeByMemberId.get(member.getId())))
                    .avatar(avatar)
                    .masteryRate(null)
                    .lastActive(null)
                    .build();
        }).collect(Collectors.toList());
    }

    private String resolveOrgRoleLabel(String roleType) {
        if (roleType == null || roleType.isBlank()) {
            return "成员";
        }
        return switch (roleType) {
            case "HEAD_TEACHER" -> "班主任";
            case "TEACHER" -> "任课教师";
            case "STUDENT" -> "学生";
            default -> roleType;
        };
    }

    private List<OrganizationNodeVO> buildTree(List<SysOrganizationEntity> entities, Long tenantId) {
        Map<Long, OrganizationNodeVO> map = new HashMap<>();
        List<OrganizationNodeVO> roots = new ArrayList<>();

        for (SysOrganizationEntity entity : entities) {
            OrganizationNodeVO vo = new OrganizationNodeVO();
            vo.setId(entity.getId());
            vo.setTenantId(entity.getTenantId());
            vo.setParentId(entity.getParentId());
            vo.setOrgType(entity.getOrgType());
            vo.setOrgPath(entity.getOrgPath());
            vo.setName(entity.getName());
            vo.setSortOrder(entity.getSortOrder());
            // 真实统计本组织下分配的成员人数
            long memberCount = sysMemberOrgDao.countByOrgId(tenantId, entity.getId());
            vo.setMemberCount((int) memberCount);
            map.put(entity.getId(), vo);
        }

        for (OrganizationNodeVO node : map.values()) {
            if (node.getParentId() == null || node.getParentId() == 0 || !map.containsKey(node.getParentId())) {
                roots.add(node);
            } else {
                OrganizationNodeVO parent = map.get(node.getParentId());
                parent.getChildren().add(node);
            }
        }
        return roots;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createNode(Long tenantId, String name, String orgType, Long parentId, Integer sortOrder) {
        Long resolvedTenantId = resolveAndVerifyTenantId(tenantId);

        // 若指定了父节点，校验父节点是否同属当前租户
        if (parentId != null && parentId > 0) {
            SysOrganizationEntity parent = sysOrganizationDao.findByIdAndTenantId(parentId, resolvedTenantId);
            if (parent == null) {
                throw new BusinessException(ResultCode.VALIDATE_FAILED.getCode(), "上级组织节点不存在或不属于当前学校");
            }
        }

        SysOrganizationEntity entity = new SysOrganizationEntity();
        entity.setTenantId(resolvedTenantId);
        entity.setName(name);
        entity.setOrgType(orgType);
        entity.setParentId(parentId != null ? parentId : 0L);
        entity.setSortOrder(sortOrder != null ? sortOrder : 0);
        entity.setOrgPath(parentId != null && parentId > 0 ? String.valueOf(parentId) : "0");
        sysOrganizationDao.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateNode(Long id, String name, Integer sortOrder) {
        Long tenantId = TenantContext.requireTenantId();
        SysOrganizationEntity entity = sysOrganizationDao.findByIdAndTenantId(id, tenantId);
        if (entity == null) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND.getCode(), "组织节点不存在或无权修改其他学校组织");
        }
        entity.setName(name);
        if (sortOrder != null) {
            entity.setSortOrder(sortOrder);
        }
        sysOrganizationDao.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteNode(Long id) {
        Long tenantId = TenantContext.requireTenantId();
        SysOrganizationEntity entity = sysOrganizationDao.findByIdAndTenantId(id, tenantId);
        if (entity == null) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND.getCode(), "组织节点不存在或无权删除");
        }

        // 校验是否存在子节点
        long childrenCount = sysOrganizationDao.countChildren(tenantId, id);
        if (childrenCount > 0) {
            throw new BusinessException(ResultCode.VALIDATE_FAILED.getCode(), "该组织节点下仍存在子组织，请先移除下级节点");
        }

        // 校验是否存在关联成员
        long memberCount = sysMemberOrgDao.countByOrgId(tenantId, id);
        if (memberCount > 0) {
            throw new BusinessException(ResultCode.VALIDATE_FAILED.getCode(), "该组织节点下仍关联 " + memberCount + " 名在册成员，请先转移或解绑");
        }

        sysOrganizationDao.deleteByIdAndTenantId(id, tenantId);
    }

    // --- OrganizationQueryApi 跨模块实现 (真实持久化数据源) ---

    @Override
    public List<Map<String, Object>> getOrganizationTree(Long tenantId) {
        List<OrganizationNodeVO> tree = getTree(tenantId);
        return tree.stream().map(node -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", node.getId());
            map.put("name", node.getName());
            map.put("orgType", node.getOrgType());
            map.put("memberCount", node.getMemberCount());
            return map;
        }).collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> listOrgsByMemberId(Long tenantId, Long memberId) {
        Long resolvedTenantId = resolveAndVerifyTenantId(tenantId);
        List<SysMemberOrgEntity> relations = sysMemberOrgDao.listByMemberId(resolvedTenantId, memberId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (SysMemberOrgEntity rel : relations) {
            SysOrganizationEntity org = sysOrganizationDao.findByIdAndTenantId(rel.getOrganizationId(), resolvedTenantId);
            if (org != null) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", org.getId());
                map.put("name", org.getName());
                map.put("type", org.getOrgType());
                map.put("roleType", rel.getRoleType());
                result.add(map);
            }
        }
        return result;
    }

    @Override
    public List<Long> listMemberIdsByOrgId(Long tenantId, Long organizationId) {
        Long resolvedTenantId = resolveAndVerifyTenantId(tenantId);
        return sysMemberOrgDao.listMemberIdsByOrgId(resolvedTenantId, organizationId);
    }
}
