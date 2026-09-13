package com.edumind.system.service;

import com.edumind.system.vo.tenant.OrganizationMemberVO;
import com.edumind.system.vo.tenant.OrganizationNodeVO;

import java.util.List;

public interface SysOrganizationService {

    List<OrganizationNodeVO> getTree(Long tenantId);

    List<OrganizationMemberVO> getOrgMembers(Long tenantId, Long orgId);

    Long createNode(Long tenantId, String name, String orgType, Long parentId, Integer sortOrder);

    void updateNode(Long id, String name, Integer sortOrder);

    void deleteNode(Long id);
}
