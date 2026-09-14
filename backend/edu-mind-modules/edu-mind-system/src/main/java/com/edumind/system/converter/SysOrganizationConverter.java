package com.edumind.system.converter;

import com.edumind.system.entity.SysOrganizationEntity;
import com.edumind.system.entity.SysTenantMemberEntity;
import com.edumind.system.entity.UserEntity;
import com.edumind.system.vo.tenant.OrganizationMemberVO;
import com.edumind.system.vo.tenant.OrganizationNodeVO;
import com.edumind.system.vo.tenant.SysTenantMemberCandidateVO;
import org.springframework.stereotype.Component;

@Component
public class SysOrganizationConverter {

    public OrganizationNodeVO toNodeVO(SysOrganizationEntity entity, int memberCount) {
        if (entity == null) return null;
        OrganizationNodeVO vo = new OrganizationNodeVO();
        vo.setId(entity.getId());
        vo.setTenantId(entity.getTenantId());
        vo.setParentId(entity.getParentId());
        vo.setOrgType(entity.getOrgType());
        vo.setOrgPath(entity.getOrgPath());
        vo.setName(entity.getName());
        vo.setSortOrder(entity.getSortOrder());
        vo.setMemberCount(memberCount);
        return vo;
    }

    public OrganizationMemberVO toMemberVO(
            SysTenantMemberEntity member,
            UserEntity user,
            String roleLabel,
            Integer masteryRate,
            String lastActive) {
        if (member == null) return null;
        String displayName = member.getRealName() != null && !member.getRealName().isBlank()
                ? member.getRealName()
                : (user != null && user.getRealName() != null ? user.getRealName() : "学员 " + member.getUserId());
        String avatar = (user != null && user.getAvatar() != null && !user.getAvatar().isBlank())
                ? user.getAvatar()
                : "https://api.dicebear.com/7.x/avataaars/svg?seed=" + displayName;

        return OrganizationMemberVO.builder()
                .id(member.getId())
                .userId(member.getUserId())
                .studentNo(member.getMemberNo() != null ? member.getMemberNo() : "STU-" + member.getUserId())
                .name(displayName)
                .role(roleLabel)
                .avatar(avatar)
                .masteryRate(masteryRate)
                .lastActive(lastActive)
                .build();
    }

    public SysTenantMemberCandidateVO toCandidateVO(
            SysTenantMemberEntity member,
            UserEntity user,
            boolean isAssigned,
            String currentRole) {
        if (member == null) return null;
        String realName = member.getRealName() != null && !member.getRealName().isBlank()
                ? member.getRealName()
                : (user != null ? user.getRealName() : "");
        String avatar = user != null && user.getAvatar() != null && !user.getAvatar().isBlank()
                ? user.getAvatar()
                : "https://api.dicebear.com/7.x/avataaars/svg?seed=" + (realName.isBlank() ? "User" : realName);

        return SysTenantMemberCandidateVO.builder()
                .memberId(member.getId())
                .userId(member.getUserId())
                .memberNo(member.getMemberNo() != null ? member.getMemberNo() : "")
                .realName(realName)
                .username(user != null ? user.getUsername() : "")
                .avatar(avatar)
                .phone(user != null ? user.getPhone() : "")
                .isAssigned(isAssigned)
                .currentRole(currentRole)
                .build();
    }
}
