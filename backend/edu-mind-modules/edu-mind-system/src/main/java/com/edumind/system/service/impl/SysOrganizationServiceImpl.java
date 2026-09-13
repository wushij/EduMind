package com.edumind.system.service.impl;

import com.edumind.common.api.ResultCode;
import com.edumind.common.context.TenantContext;
import com.edumind.common.exception.BusinessException;
import com.edumind.system.api.OrganizationQueryApi;
import com.edumind.system.dao.SysMemberOrgDao;
import com.edumind.system.dao.SysOrganizationDao;
import com.edumind.system.entity.SysMemberOrgEntity;
import com.edumind.system.entity.SysOrganizationEntity;
import com.edumind.system.service.SysOrganizationService;
import com.edumind.system.vo.tenant.OrganizationNodeVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
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

    @Override
    public List<OrganizationNodeVO> getTree(Long tenantId) {
        if (tenantId == null) {
            tenantId = TenantContext.requireTenantId();
        }
        List<SysOrganizationEntity> entities = sysOrganizationDao.listByTenantId(tenantId);
        return buildTree(entities, tenantId);
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
        if (tenantId == null) {
            tenantId = TenantContext.requireTenantId();
        }

        // 若指定了父节点，校验父节点是否同属当前租户
        if (parentId != null && parentId > 0) {
            SysOrganizationEntity parent = sysOrganizationDao.findByIdAndTenantId(parentId, tenantId);
            if (parent == null) {
                throw new BusinessException(ResultCode.VALIDATE_FAILED.getCode(), "上级组织节点不存在或不属于当前学校");
            }
        }

        SysOrganizationEntity entity = new SysOrganizationEntity();
        entity.setTenantId(tenantId);
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
        List<SysMemberOrgEntity> relations = sysMemberOrgDao.listByMemberId(tenantId, memberId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (SysMemberOrgEntity rel : relations) {
            SysOrganizationEntity org = sysOrganizationDao.findByIdAndTenantId(rel.getOrganizationId(), tenantId);
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
        return sysMemberOrgDao.listMemberIdsByOrgId(tenantId, organizationId);
    }
}
