package com.edumind;

import com.edumind.common.api.ResultCode;
import com.edumind.common.constant.SecurityConstant;
import com.edumind.common.context.TenantContext;
import com.edumind.common.exception.BusinessException;
import com.edumind.common.model.LoginUser;
import com.edumind.common.model.UserContext;
import com.edumind.course.dao.CourseMemberDao;
import com.edumind.course.entity.CourseMemberEntity;
import com.edumind.statistics.dao.intervention.TeachingInterventionDao;
import com.edumind.statistics.dto.intervention.InterventionActionDTO;
import com.edumind.statistics.dto.intervention.InterventionCreateDTO;
import com.edumind.statistics.entity.intervention.TeachingInterventionEntity;
import com.edumind.statistics.service.intervention.TeachingInterventionService;
import com.edumind.statistics.vo.intervention.TeachingInterventionVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 智教云 V2.0 · 教学干预决策 Beta 状态机与通知全链路集成测试 (Gate I8 专项工程验收)
 */
@SpringBootTest(classes = EduMindApplication.class)
@ActiveProfiles("test")
public class TeachingInterventionIntegrationTest {

    private static final Long TENANT_A = 9940L;
    private static final Long TENANT_B = 9941L;
    private static final Long USER_TEACHER_A = 99401L;
    private static final Long USER_STUDENT_A = 99402L;
    private static final Long USER_TEACHER_B = 99411L;
    private static final Long COURSE_ID_A = 8840L;

    @Autowired
    private TeachingInterventionService teachingInterventionService;

    @Autowired
    private TeachingInterventionDao teachingInterventionDao;

    @Autowired
    private CourseMemberDao courseMemberDao;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    public void setUp() {
        cleanTestData();

        // 默认初始化 Tenant A 教师用户上下文
        TenantContext.setTenantId(TENANT_A);
        LoginUser user = LoginUser.builder()
                .id(USER_TEACHER_A)
                .username("gateI8_teacher_a")
                .roles(List.of(SecurityConstant.ROLE_TEACHER))
                .permissions(List.of("analytics:intervention:view", "analytics:intervention:manage"))
                .build();
        UserContext.set(user);
    }

    @AfterEach
    public void tearDown() {
        cleanTestData();
        TenantContext.clear();
        UserContext.clear();
    }

    private void cleanTestData() {
        TenantContext.clear();
        jdbcTemplate.update("DELETE FROM sys_notification WHERE tenant_id IN (?, ?)", TENANT_A, TENANT_B);
        jdbcTemplate.update("DELETE FROM teaching_intervention WHERE tenant_id IN (?, ?)", TENANT_A, TENANT_B);
        jdbcTemplate.update("DELETE FROM course_member WHERE course_id = ?", COURSE_ID_A);
    }

    private InterventionCreateDTO buildSampleCreateDTO() {
        InterventionCreateDTO dto = new InterventionCreateDTO();
        dto.setCourseId(COURSE_ID_A);
        dto.setCourseName("高等数学（上）");
        dto.setTriggerType("EXAM_WEAK");
        dto.setTitle("第3章微分方程掌握度预警");
        dto.setProposalText("AI 检测到高数作业失分率偏高，建议批量推送专项攻坚微课与 5 道靶向等价代换习题。");
        dto.setAffectedStudentCount(12);
        dto.setCustomQuestionIds(List.of(101L, 102L));
        return dto;
    }

    @Test
    @DisplayName("Gate I8 用例 1: 创建干预提案并持久化，初始状态为 PENDING")
    public void test01_createIntervention_shouldBePending() {
        InterventionCreateDTO dto = buildSampleCreateDTO();
        TeachingInterventionVO vo = teachingInterventionService.createIntervention(dto);

        Assertions.assertNotNull(vo);
        Assertions.assertNotNull(vo.getId());
        Assertions.assertEquals("PENDING", vo.getStatus());
        Assertions.assertEquals("第3章微分方程掌握度预警", vo.getTitle());
        Assertions.assertEquals(COURSE_ID_A, vo.getCourseId());

        // 数据库校验
        TeachingInterventionEntity entity = teachingInterventionDao.findByIdAndTenantId(vo.getId(), TENANT_A);
        Assertions.assertNotNull(entity);
        Assertions.assertEquals("PENDING", entity.getStatus());
        Assertions.assertEquals(TENANT_A, entity.getTenantId());
    }

    @Test
    @DisplayName("Gate I8 用例 2: 审核通过干预提案，状态流转为 APPROVED 且记录审核人")
    public void test02_approveIntervention_shouldBeApproved() {
        InterventionCreateDTO createDTO = buildSampleCreateDTO();
        TeachingInterventionVO vo = teachingInterventionService.createIntervention(createDTO);

        InterventionActionDTO actionDTO = new InterventionActionDTO();
        actionDTO.setRemark("已审核，建议尽快推送");
        actionDTO.setCustomQuestionIds(List.of(201L, 202L));

        teachingInterventionService.approveIntervention(vo.getId(), actionDTO);

        TeachingInterventionEntity entity = teachingInterventionDao.findByIdAndTenantId(vo.getId(), TENANT_A);
        Assertions.assertNotNull(entity);
        Assertions.assertEquals("APPROVED", entity.getStatus());
        Assertions.assertEquals(USER_TEACHER_A, entity.getApprovedBy());
    }

    @Test
    @DisplayName("Gate I8 用例 3: 教师驳回/忽略干预提案，状态流转为 REVOKED")
    public void test03_rejectIntervention_shouldBeRevoked() {
        InterventionCreateDTO createDTO = buildSampleCreateDTO();
        TeachingInterventionVO vo = teachingInterventionService.createIntervention(createDTO);

        teachingInterventionService.rejectIntervention(vo.getId());

        TeachingInterventionEntity entity = teachingInterventionDao.findByIdAndTenantId(vo.getId(), TENANT_A);
        Assertions.assertNotNull(entity);
        Assertions.assertEquals("REVOKED", entity.getStatus());
    }

    @Test
    @DisplayName("Gate I8 用例 4: 审核后分发干预，状态变更为 DISPATCHED 并联动通知触达目标学生")
    public void test04_dispatchApprovedIntervention_shouldSendNotification() {
        // 1. 注册学生成员至课程
        CourseMemberEntity member = new CourseMemberEntity();
        member.setCourseId(COURSE_ID_A);
        member.setUserId(USER_STUDENT_A);
        member.setMemberRole("STUDENT");
        member.setCreateTime(LocalDateTime.now());
        courseMemberDao.insert(member);

        // 2. 创建并批准干预
        InterventionCreateDTO createDTO = buildSampleCreateDTO();
        TeachingInterventionVO vo = teachingInterventionService.createIntervention(createDTO);
        teachingInterventionService.approveIntervention(vo.getId(), null);

        // 3. 执行分发
        teachingInterventionService.dispatchIntervention(vo.getId());

        // 4. 校验干预实体状态
        TeachingInterventionEntity entity = teachingInterventionDao.findByIdAndTenantId(vo.getId(), TENANT_A);
        Assertions.assertNotNull(entity);
        Assertions.assertEquals("DISPATCHED", entity.getStatus());

        // 5. 校验 sys_notification 记录
        List<Map<String, Object>> notifications = jdbcTemplate.queryForList(
                "SELECT * FROM sys_notification WHERE tenant_id = ? AND type = 'INTERVENTION' AND ref_id = ?",
                TENANT_A, vo.getId()
        );
        Assertions.assertFalse(notifications.isEmpty(), "分发后应在 sys_notification 生成学生通知");
        Map<String, Object> notification = notifications.get(0);
        Assertions.assertEquals(USER_STUDENT_A, ((Number) notification.get("user_id")).longValue());
        Assertions.assertEquals(TENANT_A, ((Number) notification.get("tenant_id")).longValue());
        Assertions.assertTrue(((String) notification.get("title")).contains("第3章微分方程掌握度预警"));
    }

    @Test
    @DisplayName("Gate I8 用例 5: 跨租户 IDOR 越权拦截，抛出 403 FORBIDDEN")
    public void test05_crossTenantIntervention_shouldBeForbidden() {
        // 在 Tenant A 下创建干预
        InterventionCreateDTO createDTO = buildSampleCreateDTO();
        TeachingInterventionVO vo = teachingInterventionService.createIntervention(createDTO);
        Long interventionId = vo.getId();

        // 切换至 Tenant B 教师上下文
        TenantContext.setTenantId(TENANT_B);
        LoginUser userB = LoginUser.builder()
                .id(USER_TEACHER_B)
                .username("gateI8_teacher_b")
                .roles(List.of(SecurityConstant.ROLE_TEACHER))
                .permissions(List.of("analytics:intervention:manage"))
                .build();
        UserContext.set(userB);

        // 跨租户审批应被拦截
        BusinessException exApprove = Assertions.assertThrows(BusinessException.class, () ->
                teachingInterventionService.approveIntervention(interventionId, null)
        );
        Assertions.assertEquals(ResultCode.FORBIDDEN.getCode(), exApprove.getCode());

        // 跨租户分发应被拦截
        BusinessException exDispatch = Assertions.assertThrows(BusinessException.class, () ->
                teachingInterventionService.dispatchIntervention(interventionId)
        );
        Assertions.assertEquals(ResultCode.FORBIDDEN.getCode(), exDispatch.getCode());

        // 跨租户驳回应被拦截
        BusinessException exReject = Assertions.assertThrows(BusinessException.class, () ->
                teachingInterventionService.rejectIntervention(interventionId)
        );
        Assertions.assertEquals(ResultCode.FORBIDDEN.getCode(), exReject.getCode());
    }

    @Test
    @DisplayName("Gate I8 用例 6: 非法状态机流转防御（重复审批 / 未审批直接分发抛出业务异常）")
    public void test06_invalidStateTransitions_shouldThrowBusinessException() {
        InterventionCreateDTO createDTO = buildSampleCreateDTO();
        TeachingInterventionVO vo = teachingInterventionService.createIntervention(createDTO);
        Long id = vo.getId();

        // 1. 未审批直接分发 -> 预期抛出校验异常
        BusinessException exPrematureDispatch = Assertions.assertThrows(BusinessException.class, () ->
                teachingInterventionService.dispatchIntervention(id)
        );
        Assertions.assertEquals(ResultCode.VALIDATE_FAILED.getCode(), exPrematureDispatch.getCode());

        // 2. 正常审批
        teachingInterventionService.approveIntervention(id, null);

        // 3. 重复审批 -> 预期抛出校验异常
        BusinessException exDuplicateApprove = Assertions.assertThrows(BusinessException.class, () ->
                teachingInterventionService.approveIntervention(id, null)
        );
        Assertions.assertEquals(ResultCode.VALIDATE_FAILED.getCode(), exDuplicateApprove.getCode());
    }
}
