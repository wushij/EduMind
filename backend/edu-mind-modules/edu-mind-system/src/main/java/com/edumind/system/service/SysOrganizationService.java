package com.edumind.system.service;

import com.edumind.system.dto.tenant.OrgMemberAssignDTO;
import com.edumind.system.dto.tenant.OrgMemberBatchAssignDTO;
import com.edumind.system.vo.tenant.OrganizationMemberVO;
import com.edumind.system.vo.tenant.OrganizationNodeVO;
import com.edumind.system.vo.tenant.SysOrgNodeStatsVO;
import com.edumind.system.vo.tenant.SysOrgStatsVO;
import com.edumind.system.vo.tenant.SysTenantMemberCandidateVO;

import java.util.List;
import java.util.Map;

public interface SysOrganizationService {

    List<OrganizationNodeVO> getTree(Long tenantId);

    List<OrganizationMemberVO> getOrgMembers(Long tenantId, Long orgId);

    void assignMember(Long tenantId, Long orgId, OrgMemberAssignDTO dto);

    void removeMember(Long tenantId, Long orgId, Long memberId);

    Long createNode(Long tenantId, String name, String orgType, Long parentId, Integer sortOrder);

    void updateNode(Long id, String name, Integer sortOrder);

    void deleteNode(Long id);

    SysOrgStatsVO getTenantOrgStats(Long tenantId);

    SysOrgNodeStatsVO getNodeStats(Long tenantId, Long orgId);

    List<SysTenantMemberCandidateVO> getCandidateMembers(Long tenantId, Long orgId, String keyword);

    void batchAssignMembers(Long tenantId, Long orgId, OrgMemberBatchAssignDTO dto);

    Map<String, Object> getStudentCognitiveProfile(Long tenantId, Long studentUserId);
}

