package com.edumind;

import cn.dev33.satoken.stp.StpUtil;
import com.edumind.common.context.TenantContext;
import com.edumind.common.exception.BusinessException;
import com.edumind.system.api.TenantDataScope;
import com.edumind.system.api.TenantDataScopeApi;
import com.edumind.system.dao.SysMemberOrgDao;
import com.edumind.system.dao.SysOrganizationDao;
import com.edumind.system.dao.SysTenantMemberDao;
import com.edumind.system.dto.tenant.OrgMemberAssignDTO;
import com.edumind.system.entity.SysOrganizationEntity;
import com.edumind.system.entity.SysTenantMemberEntity;
import com.edumind.system.service.SysOrganizationService;
import com.edumind.system.vo.tenant.OrganizationMemberVO;
import com.edumind.system.vo.tenant.OrganizationNodeVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

/**
 * 智教云 V2.0 · 组织架构与 RBAC 数据范围集成测试 (Gate I3)
 */
@SpringBootTest(classes = EduMindApplication.class)
@ActiveProfiles("test")
public class OrganizationRbacIntegrationTest {

    @Autowired
    private SysOrganizationService sysOrganizationService;

    @Autowired
    private SysOrganizationDao sysOrganizationDao;

    @Autowired
    private SysTenantMemberDao sysTenantMemberDao;

    @Autowired
    private SysMemberOrgDao sysMemberOrgDao;

    @Autowired
    private TenantDataScopeApi tenantDataScopeApi;

    @Autowired
    private com.edumind.system.dao.SysTenantDao sysTenantDao;

    private static final Long TENANT_A = 7001L;
    private static final Long TENANT_B = 7002L;

    @BeforeEach
    void setUp() {
        cleanData();
    }

    @AfterEach
    void tearDown() {
        cleanData();
    }

    private void cleanData() {
        TenantContext.runWithoutTenant(() -> {
            ensureTenant(TENANT_A, "TENANT_7001", "测试学校7001");
            ensureTenant(TENANT_B, "TENANT_7002", "测试学校7002");
            // 严格外键清理顺序: member_org -> sys_organization -> sys_tenant_member
            sysMemberOrgDao.deleteByTenantId(TENANT_A);
            sysMemberOrgDao.deleteByTenantId(TENANT_B);
            sysOrganizationDao.deleteByTenantId(TENANT_A);
            sysOrganizationDao.deleteByTenantId(TENANT_B);
            sysTenantMemberDao.deleteByTenantId(TENANT_A);
            sysTenantMemberDao.deleteByTenantId(TENANT_B);
        });
        TenantContext.clear();
    }

    private void ensureTenant(Long tenantId, String code, String name) {
        com.edumind.system.entity.SysTenantEntity tenant = sysTenantDao.findById(tenantId);
        if (tenant == null) {
            tenant = new com.edumind.system.entity.SysTenantEntity();
            tenant.setId(tenantId);
            tenant.setCode(code);
            tenant.setName(name);
            tenant.setStatus(1);
            tenant.setPlanCode("PRO");
            tenant.setCreateTime(java.time.LocalDateTime.now());
            tenant.setUpdateTime(java.time.LocalDateTime.now());
            sysTenantDao.insert(tenant);
        }
    }

    @Test
    @DisplayName("测试组织成员真实持久化分配、查询、幂等更新与解绑")
    void testOrgMemberAssignAndQuery() {
        TenantContext.setTenantId(TENANT_A);

        // 1. 创建测试节点
        Long orgId = sysOrganizationService.createNode(TENANT_A, "高一年级组", "FACULTY", 0L, 1);
        Assertions.assertNotNull(orgId);

        // 2. 准备租户 A 的测试成员
        SysTenantMemberEntity memberA = new SysTenantMemberEntity();
        memberA.setTenantId(TENANT_A);
        memberA.setUserId(991L);
        memberA.setMemberNo("T991");
        memberA.setRealName("张老师");
        memberA.setStatus(1);
        memberA.setIsDefault(1);
        sysTenantMemberDao.insert(memberA);

        // 3. 分配张老师为任课教师
        OrgMemberAssignDTO assignDTO = new OrgMemberAssignDTO();
        assignDTO.setMemberId(memberA.getId());
        assignDTO.setRoleType("TEACHER");
        sysOrganizationService.assignMember(TENANT_A, orgId, assignDTO);

        // 4. 查询成员名单，验证真实关联与已修复的 resolvedTenantId 查询
        List<OrganizationMemberVO> members = sysOrganizationService.getOrgMembers(TENANT_A, orgId);
        Assertions.assertEquals(1, members.size(), "分配后应可查询到 1 名成员");
        Assertions.assertEquals("任课教师", members.get(0).getRole());
        Assertions.assertEquals("张老师", members.get(0).getName());

        // 5. 幂等更新测试：更新角色为班主任，验证不重复插入
        assignDTO.setRoleType("HEAD_TEACHER");
        sysOrganizationService.assignMember(TENANT_A, orgId, assignDTO);
        members = sysOrganizationService.getOrgMembers(TENANT_A, orgId);
        Assertions.assertEquals(1, members.size(), "幂等更新后成员数量仍应为 1");
        Assertions.assertEquals("班主任", members.get(0).getRole());

        // 6. 成员解绑移出测试
        sysOrganizationService.removeMember(TENANT_A, orgId, memberA.getId());
        members = sysOrganizationService.getOrgMembers(TENANT_A, orgId);
        Assertions.assertTrue(members.isEmpty(), "移出后组织成员名单应为空");
    }

    @Test
    @DisplayName("测试组织成员分配与跨租户越权阻断 (403 / Fail-Closed)")
    void testCrossTenantForbidden() {
        TenantContext.setTenantId(TENANT_A);
        Long orgIdA = sysOrganizationService.createNode(TENANT_A, "A校教研组", "DEPT", 0L, 1);

        // 准备属于租户 B 的成员
        SysTenantMemberEntity memberB = new SysTenantMemberEntity();
        memberB.setTenantId(TENANT_B);
        memberB.setUserId(992L);
        memberB.setMemberNo("T992");
        memberB.setRealName("B校李老师");
        memberB.setStatus(1);
        memberB.setIsDefault(1);
        sysTenantMemberDao.insert(memberB);

        // 试图将 B 校成员分配进 A 校组织 -> 必须抛出 403 业务异常
        OrgMemberAssignDTO assignDTO = new OrgMemberAssignDTO();
        assignDTO.setMemberId(memberB.getId());
        assignDTO.setRoleType("TEACHER");

        Assertions.assertThrows(BusinessException.class, () -> {
            sysOrganizationService.assignMember(TENANT_A, orgIdA, assignDTO);
        }, "分配非本校成员应被安全拦截");

        // 显式传入非法租户 ID -> 必须拦截
        Assertions.assertThrows(BusinessException.class, () -> {
            sysOrganizationService.getOrgMembers(TENANT_B, orgIdA);
        }, "跨租户查询组织成员应被拦截");
    }

    @Test
    @DisplayName("测试五级角色数据范围 (TenantDataScope) 组织树隔离过滤")
    void testDataScopeFilteredTree() {
        TenantContext.setTenantId(TENANT_A);

        // 创建多级组织树结构: 校区(1) -> 学院(2) -> 班级(3)，以及同级的另一个校区(4)
        Long campus1 = sysOrganizationService.createNode(TENANT_A, "西校区", "CAMPUS", 0L, 1);
        Long college1 = sysOrganizationService.createNode(TENANT_A, "软件工程学院", "FACULTY", campus1, 1);
        Long class1 = sysOrganizationService.createNode(TENANT_A, "软工2601班", "CLASS", college1, 1);
        Long campus2 = sysOrganizationService.createNode(TENANT_A, "东校区", "CAMPUS", 0L, 2);

        // 准备院系管理员成员，只绑定到 college1 (学院节点)
        SysTenantMemberEntity adminMember = new SysTenantMemberEntity();
        adminMember.setTenantId(TENANT_A);
        adminMember.setUserId(888L);
        adminMember.setMemberNo("ORG_ADMIN_888");
        adminMember.setRealName("王系主任");
        adminMember.setStatus(1);
        adminMember.setIsDefault(1);
        sysTenantMemberDao.insert(adminMember);

        OrgMemberAssignDTO assignDTO = new OrgMemberAssignDTO();
        assignDTO.setMemberId(adminMember.getId());
        assignDTO.setRoleType("HEAD_TEACHER"); // 具备管理权限
        sysOrganizationService.assignMember(TENANT_A, college1, assignDTO);

        // 解析数据范围
        TenantDataScope scope = tenantDataScopeApi.resolve(888L, TENANT_A);
        Assertions.assertFalse(scope.isAllTenant(), "院系负责人不具备全租户视角");
        Assertions.assertTrue(scope.getOrgIds().contains(college1), "可见范围应包含所属学院");
        Assertions.assertTrue(scope.getOrgIds().contains(class1), "可见范围应递归展开包含下级班级");
        Assertions.assertFalse(scope.getOrgIds().contains(campus2), "可见范围不得包含无关的东校区");
    }
}
