package com.edumind;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edumind.common.api.ResultCode;
import com.edumind.common.context.TenantContext;
import com.edumind.common.exception.BusinessException;
import com.edumind.common.model.LoginUser;
import com.edumind.common.model.UserContext;
import com.edumind.notification.dao.NotificationBroadcastDao;
import com.edumind.notification.dao.NotificationDao;
import com.edumind.notification.dto.broadcast.BroadcastCreateDTO;
import com.edumind.notification.entity.NotificationBroadcastEntity;
import com.edumind.notification.entity.NotificationEntity;
import com.edumind.notification.mapper.NotificationBroadcastMapper;
import com.edumind.notification.mapper.NotificationMapper;
import com.edumind.notification.service.broadcast.NotificationBroadcastService;
import com.edumind.notification.service.notification.NotificationService;
import com.edumind.notification.vo.broadcast.BroadcastEstimateVO;
import com.edumind.notification.vo.broadcast.NotificationBroadcastVO;
import com.edumind.notification.vo.notification.NotificationVO;
import com.edumind.system.dao.RoleDao;
import com.edumind.system.dao.SysTenantDao;
import com.edumind.system.dao.SysTenantMemberDao;
import com.edumind.system.dao.UserDao;
import com.edumind.system.dao.UserRoleDao;
import com.edumind.system.entity.RoleEntity;
import com.edumind.system.entity.SysTenantEntity;
import com.edumind.system.entity.SysTenantMemberEntity;
import com.edumind.system.entity.UserEntity;
import com.edumind.system.entity.UserRoleEntity;
import com.edumind.system.mapper.SysTenantMemberMapper;
import com.edumind.system.mapper.UserRoleMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * 智教云 V2.0 · 通知广播多租户触达全链路闭环集成测试 (Gate I5 专项工程验收)
 */
@SpringBootTest(classes = EduMindApplication.class)
@ActiveProfiles("test")
public class NotificationBroadcastIntegrationTest {

    @Autowired
    private NotificationBroadcastService broadcastService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationBroadcastDao broadcastDao;

    @Autowired
    private NotificationDao notificationDao;

    @Autowired
    private SysTenantDao sysTenantDao;

    @Autowired
    private SysTenantMemberDao sysTenantMemberDao;

    @Autowired
    private UserDao userDao;

    @Autowired
    private UserRoleDao userRoleDao;

    @Autowired
    private RoleDao roleDao;

    @Autowired
    private NotificationMapper notificationMapper;

    @Autowired
    private NotificationBroadcastMapper broadcastMapper;

    @Autowired
    private SysTenantMemberMapper tenantMemberMapper;

    @Autowired
    private UserRoleMapper userRoleMapper;

    private static final Long TENANT_A = 9910L;
    private static final Long TENANT_B = 9911L;

    private static final Long ADMIN_A = 9101L;
    private static final Long TEACHER_A = 9102L;
    private static final Long STUDENT_A = 9103L;

    private static final Long ADMIN_B = 9111L;
    private static final Long STUDENT_B = 9112L;

    @BeforeEach
    void setUp() {
        cleanData();
        prepareTestData();
    }

    @AfterEach
    void tearDown() {
        cleanData();
    }

    private void cleanData() {
        TenantContext.runWithoutTenant(() -> {
            // 1. 清理测试通知记录
            notificationMapper.delete(new LambdaQueryWrapper<NotificationEntity>()
                    .in(NotificationEntity::getTenantId, TENANT_A, TENANT_B));
            notificationMapper.delete(new LambdaQueryWrapper<NotificationEntity>()
                    .in(NotificationEntity::getUserId, ADMIN_A, TEACHER_A, STUDENT_A, ADMIN_B, STUDENT_B));

            // 2. 清理测试广播任务
            broadcastMapper.delete(new LambdaQueryWrapper<NotificationBroadcastEntity>()
                    .in(NotificationBroadcastEntity::getTenantId, TENANT_A, TENANT_B));

            // 3. 清理租户成员关系
            tenantMemberMapper.delete(new LambdaQueryWrapper<SysTenantMemberEntity>()
                    .in(SysTenantMemberEntity::getTenantId, TENANT_A, TENANT_B));
            tenantMemberMapper.delete(new LambdaQueryWrapper<SysTenantMemberEntity>()
                    .in(SysTenantMemberEntity::getUserId, ADMIN_A, TEACHER_A, STUDENT_A, ADMIN_B, STUDENT_B));

            // 4. 清理用户角色关联
            userRoleMapper.delete(new LambdaQueryWrapper<UserRoleEntity>()
                    .in(UserRoleEntity::getUserId, ADMIN_A, TEACHER_A, STUDENT_A, ADMIN_B, STUDENT_B));
        });
        TenantContext.clear();
        UserContext.clear();
    }

    private void prepareTestData() {
        TenantContext.runWithoutTenant(() -> {
            ensureTenant(TENANT_A, "TENANT_9910", "测试大学9910");
            ensureTenant(TENANT_B, "TENANT_9911", "测试学院9911");

            // 角色 ID: 1-ADMIN, 2-TEACHER, 3-STUDENT, 4-TENANT_ADMIN
            RoleEntity adminRole = roleDao.findByRoleCode("ADMIN");
            RoleEntity teacherRole = roleDao.findByRoleCode("TEACHER");
            RoleEntity studentRole = roleDao.findByRoleCode("STUDENT");
            Long adminRoleId = adminRole != null ? adminRole.getId() : 1L;
            Long teacherRoleId = teacherRole != null ? teacherRole.getId() : 2L;
            Long studentRoleId = studentRole != null ? studentRole.getId() : 3L;

            // 租户 A 成员初始化 (ADMIN_A, TEACHER_A, STUDENT_A)
            ensureUser(ADMIN_A, "admin_a", "A校管理员");
            ensureTenantMember(TENANT_A, ADMIN_A, "M9101", "A校管理员");
            ensureUserRole(ADMIN_A, adminRoleId);

            ensureUser(TEACHER_A, "teacher_a", "A校张老师");
            ensureTenantMember(TENANT_A, TEACHER_A, "M9102", "A校张老师");
            ensureUserRole(TEACHER_A, teacherRoleId);

            ensureUser(STUDENT_A, "student_a", "A校李同学");
            ensureTenantMember(TENANT_A, STUDENT_A, "M9103", "A校李同学");
            ensureUserRole(STUDENT_A, studentRoleId);

            // 租户 B 成员初始化 (ADMIN_B, STUDENT_B)
            ensureUser(ADMIN_B, "admin_b", "B校管理员");
            ensureTenantMember(TENANT_B, ADMIN_B, "M9111", "B校管理员");
            ensureUserRole(ADMIN_B, adminRoleId);

            ensureUser(STUDENT_B, "student_b", "B校王同学");
            ensureTenantMember(TENANT_B, STUDENT_B, "M9112", "B校王同学");
            ensureUserRole(STUDENT_B, studentRoleId);
        });
        TenantContext.clear();
        UserContext.clear();
    }

    private void ensureTenant(Long tenantId, String code, String name) {
        SysTenantEntity tenant = sysTenantDao.findById(tenantId);
        if (tenant == null) {
            tenant = new SysTenantEntity();
            tenant.setId(tenantId);
            tenant.setCode(code);
            tenant.setName(name);
            tenant.setStatus(1);
            tenant.setPlanCode("PRO");
            tenant.setCreateTime(LocalDateTime.now());
            tenant.setUpdateTime(LocalDateTime.now());
            sysTenantDao.insert(tenant);
        }
    }

    private void ensureUser(Long userId, String username, String realName) {
        UserEntity user = userDao.findById(userId);
        if (user == null) {
            user = new UserEntity();
            user.setId(userId);
            user.setUsername(username);
            user.setRealName(realName);
            user.setPassword("123456");
            user.setStatus("1");
            user.setCreateTime(LocalDateTime.now());
            userDao.insert(user);
        }
    }

    private void ensureTenantMember(Long tenantId, Long userId, String memberNo, String realName) {
        SysTenantMemberEntity member = sysTenantMemberDao.findByTenantAndUser(tenantId, userId);
        if (member == null) {
            member = new SysTenantMemberEntity();
            member.setTenantId(tenantId);
            member.setUserId(userId);
            member.setMemberNo(memberNo);
            member.setRealName(realName);
            member.setStatus(1);
            member.setIsDefault(1);
            member.setCreateTime(LocalDateTime.now());
            sysTenantMemberDao.insert(member);
        }
    }

    private void ensureUserRole(Long userId, Long roleId) {
        userRoleDao.deleteByUserId(userId);
        UserRoleEntity entity = new UserRoleEntity();
        entity.setUserId(userId);
        entity.setRoleId(roleId);
        userRoleDao.insert(entity);
    }

    private void mockLogin(Long tenantId, Long userId, String roleCode, List<String> permissions) {
        TenantContext.setTenantId(tenantId);
        UserContext.set(LoginUser.builder()
                .id(userId)
                .username("user_" + userId)
                .realName("测试用户" + userId)
                .roles(List.of(roleCode))
                .permissions(permissions)
                .build());
    }

    private void awaitCondition(BooleanSupplier condition, long timeoutMs) {
        long start = System.currentTimeMillis();
        while (!condition.getAsBoolean()) {
            if (System.currentTimeMillis() - start > timeoutMs) {
                throw new AssertionError("等待异步通知派发超时");
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException ignored) {
            }
        }
    }

    @Test
    @DisplayName("用例 1: 租户 A 广播 all (仅 tenant A 用户收到 notification，tenant B 完全隔离)")
    void test1_broadcastAllTenantIsolation() {
        // 1. 登录租户 A 管理员，发起全员广播
        mockLogin(TENANT_A, ADMIN_A, "TENANT_ADMIN", List.of("notice:broadcast:send", "notice:broadcast:view", "notice:view"));

        BroadcastCreateDTO dto = new BroadcastCreateDTO();
        dto.setTitle("开学第一课全员教学通知");
        dto.setContent("请各位师生务必于本周五前完成课程培养方案核对。");
        dto.setTargetType("all");
        dto.setPriority(1);

        NotificationBroadcastVO broadcastVO = broadcastService.createBroadcast(dto, ADMIN_A, "A校管理员");
        Assertions.assertNotNull(broadcastVO.getId(), "广播任务必须成功创建并生成 ID");
        Assertions.assertEquals(TENANT_A, broadcastVO.getTenantId(), "广播任务必须绑定当前发起方租户 ID");
        Assertions.assertEquals(3, broadcastVO.getTotalCount(), "租户 A 内有 3 个活跃成员 (ADMIN, TEACHER, STUDENT)");

        // 2. 异步派发等待与断言：租户 A 的教师和学生应收到通知
        awaitCondition(() -> {
            mockLogin(TENANT_A, STUDENT_A, "STUDENT", List.of("notice:view"));
            return !notificationService.listAll(STUDENT_A).isEmpty();
        }, 3000);

        // 验证 A 校教师收到通知
        mockLogin(TENANT_A, TEACHER_A, "TEACHER", List.of("notice:view"));
        List<NotificationVO> teacherList = notificationService.listAll(TEACHER_A);
        Assertions.assertEquals(1, teacherList.size(), "A 校教师应收到该广播通知");
        Assertions.assertEquals("开学第一课全员教学通知", teacherList.get(0).getTitle());
        Assertions.assertEquals(TENANT_A, teacherList.get(0).getTenantId());
        Assertions.assertEquals("BROADCAST", teacherList.get(0).getType());

        // 验证 A 校学生收到通知
        mockLogin(TENANT_A, STUDENT_A, "STUDENT", List.of("notice:view"));
        List<NotificationVO> studentListA = notificationService.listAll(STUDENT_A);
        Assertions.assertEquals(1, studentListA.size(), "A 校学生应收到该广播通知");
        Assertions.assertEquals("开学第一课全员教学通知", studentListA.get(0).getTitle());

        // 3. 验证租户 B 完全不受污染 (跨租户隔离核心断言)
        mockLogin(TENANT_B, STUDENT_B, "STUDENT", List.of("notice:view"));
        List<NotificationVO> studentListB = notificationService.listAll(STUDENT_B);
        Assertions.assertTrue(studentListB.isEmpty(), "租户 A 的全员广播绝对不可泄露或污染至租户 B 的成员");
    }

    @Test
    @DisplayName("用例 2: 租户 A 按角色广播 role=STUDENT (仅 A 校学生收到，A 校教师及 B 校学生均未接收)")
    void test2_broadcastRoleStudentScoping() {
        mockLogin(TENANT_A, ADMIN_A, "TENANT_ADMIN", List.of("notice:broadcast:send", "notice:broadcast:view"));

        BroadcastCreateDTO dto = new BroadcastCreateDTO();
        dto.setTitle("期末评教专项学生通告");
        dto.setContent("请全体同学在系统内完成本学期任课教师教学评教。");
        dto.setTargetType("role");
        dto.setTargetPayload("STUDENT");
        dto.setPriority(0);

        NotificationBroadcastVO broadcastVO = broadcastService.createBroadcast(dto, ADMIN_A, "A校管理员");
        Assertions.assertNotNull(broadcastVO.getId());
        Assertions.assertEquals(1, broadcastVO.getTotalCount(), "租户 A 仅有 1 个学生角色成员");

        // 等待派发
        awaitCondition(() -> {
            mockLogin(TENANT_A, STUDENT_A, "STUDENT", List.of("notice:view"));
            return !notificationService.listAll(STUDENT_A).isEmpty();
        }, 3000);

        // A 校学生收到
        mockLogin(TENANT_A, STUDENT_A, "STUDENT", List.of("notice:view"));
        List<NotificationVO> studentList = notificationService.listAll(STUDENT_A);
        Assertions.assertEquals(1, studentList.size(), "A 校学生应收到学生角色专属通知");
        Assertions.assertEquals("期末评教专项学生通告", studentList.get(0).getTitle());

        // A 校教师未收到 (角色精准过滤)
        mockLogin(TENANT_A, TEACHER_A, "TEACHER", List.of("notice:view"));
        List<NotificationVO> teacherList = notificationService.listAll(TEACHER_A);
        Assertions.assertTrue(teacherList.isEmpty(), "学生角色专属通知不应触达教师");

        // B 校学生未收到 (租户隔离)
        mockLogin(TENANT_B, STUDENT_B, "STUDENT", List.of("notice:view"));
        List<NotificationVO> studentListB = notificationService.listAll(STUDENT_B);
        Assertions.assertTrue(studentListB.isEmpty(), "跨租户角色广播必须严格受租户物理隔离保护");
    }

    @Test
    @DisplayName("用例 3: 用户已读回写已读统计 (单条 markAsRead 与批量 markAllAsRead 回写 broadcast.read_count)")
    void test3_markAsReadIncrementsBroadcastReadCount() {
        mockLogin(TENANT_A, ADMIN_A, "TENANT_ADMIN", List.of("notice:broadcast:send", "notice:broadcast:view"));

        // 发送第 1 条广播
        BroadcastCreateDTO dto1 = new BroadcastCreateDTO();
        dto1.setTitle("广播消息一");
        dto1.setContent("请查看消息一详情");
        dto1.setTargetType("role");
        dto1.setTargetPayload("STUDENT");
        NotificationBroadcastVO b1 = broadcastService.createBroadcast(dto1, ADMIN_A, "A校管理员");

        // 发送第 2 条广播
        BroadcastCreateDTO dto2 = new BroadcastCreateDTO();
        dto2.setTitle("广播消息二");
        dto2.setContent("请查看消息二详情");
        dto2.setTargetType("role");
        dto2.setTargetPayload("STUDENT");
        NotificationBroadcastVO b2 = broadcastService.createBroadcast(dto2, ADMIN_A, "A校管理员");

        // 等待派发完毕
        mockLogin(TENANT_A, STUDENT_A, "STUDENT", List.of("notice:view"));
        awaitCondition(() -> notificationService.listAll(STUDENT_A).size() >= 2, 3000);

        List<NotificationVO> studentNotifs = notificationService.listAll(STUDENT_A);
        Assertions.assertEquals(2, studentNotifs.size());

        NotificationVO notif1 = studentNotifs.stream()
                .filter(n -> "广播消息一".equals(n.getTitle()))
                .findFirst().orElseThrow();
        NotificationVO notif2 = studentNotifs.stream()
                .filter(n -> "广播消息二".equals(n.getTitle()))
                .findFirst().orElseThrow();

        // 1. 单条已读测试: markAsRead
        notificationService.markAsRead(notif1.getId(), STUDENT_A);
        NotificationBroadcastEntity b1AfterRead = broadcastDao.findById(b1.getId());
        Assertions.assertEquals(1, b1AfterRead.getReadCount(), "单条标记已读后，广播任务一的 read_count 应增加为 1");

        NotificationBroadcastEntity b2BeforeBatch = broadcastDao.findById(b2.getId());
        Assertions.assertEquals(0, b2BeforeBatch.getReadCount(), "未读的广播任务二 read_count 仍应为 0");

        // 2. 批量一键已读测试: markAllAsRead
        notificationService.markAllAsRead(STUDENT_A);
        NotificationBroadcastEntity b2AfterBatch = broadcastDao.findById(b2.getId());
        Assertions.assertEquals(1, b2AfterBatch.getReadCount(), "批量标记已读后，广播任务二的 read_count 应成功回写为 1");
    }

    @Test
    @DisplayName("用例 4: 跨租户广播详情查看与删除越权拦截 (Fail-Closed 403 / 数据隔离)")
    void test4_crossTenantBroadcastForbidden() {
        // 1. 租户 A 发送一条广播
        mockLogin(TENANT_A, ADMIN_A, "TENANT_ADMIN", List.of("notice:broadcast:send", "notice:broadcast:view"));
        BroadcastCreateDTO dto = new BroadcastCreateDTO();
        dto.setTitle("A校机密行政广播");
        dto.setContent("涉及A校内部教学资源配置安排");
        dto.setTargetType("all");
        NotificationBroadcastVO bA = broadcastService.createBroadcast(dto, ADMIN_A, "A校管理员");

        // 2. 切换为租户 B 管理员，尝试越权查看租户 A 的广播详情
        mockLogin(TENANT_B, ADMIN_B, "TENANT_ADMIN", List.of("notice:broadcast:send", "notice:broadcast:view"));

        BusinessException viewEx = Assertions.assertThrows(BusinessException.class, () ->
                broadcastService.getDetail(bA.getId()));
        Assertions.assertTrue(viewEx.getCode() == ResultCode.FORBIDDEN.getCode() || viewEx.getMessage().contains("不存在"),
                "跨租户读取广播详情必须被多租户物理隔离或业务越权检查拦截 (403 或不存在)");

        // 3. 租户 B 管理员尝试越权删除租户 A 的广播任务
        BusinessException deleteEx = Assertions.assertThrows(BusinessException.class, () ->
                broadcastService.deleteById(bA.getId()));
        Assertions.assertTrue(deleteEx.getCode() == ResultCode.FORBIDDEN.getCode() || deleteEx.getMessage().contains("不存在"),
                "跨租户删除广播任务必须被越权拦截");

        // 4. 切回租户 A 验证广播依然完好无损
        mockLogin(TENANT_A, ADMIN_A, "TENANT_ADMIN", List.of("notice:broadcast:send", "notice:broadcast:view"));
        NotificationBroadcastVO detailA = broadcastService.getDetail(bA.getId());
        Assertions.assertNotNull(detailA, "租户 A 的广播数据未被非法篡改或删除");
        Assertions.assertEquals("A校机密行政广播", detailA.getTitle());
    }

    @Test
    @DisplayName("用例 5: 受众人数预估 estimateAudience 与租户边界严格一致")
    void test5_estimateAudienceScoping() {
        // 1. 租户 A 预估全体受众
        mockLogin(TENANT_A, ADMIN_A, "TENANT_ADMIN", List.of("notice:broadcast:view"));
        BroadcastEstimateVO estAllA = broadcastService.estimateAudience("all", null);
        Assertions.assertEquals(3, estAllA.getEstimatedCount(), "租户 A 共有 3 个活跃成员");
        Assertions.assertTrue(estAllA.getFormattedDesc().contains("本校预计触达全体成员 3 人"),
                "预估描述文案应体现本校租户范围");

        // 2. 租户 A 预估学生角色
        BroadcastEstimateVO estStudentA = broadcastService.estimateAudience("role", "STUDENT");
        Assertions.assertEquals(1, estStudentA.getEstimatedCount(), "租户 A 仅有 1 个学生");
        Assertions.assertTrue(estStudentA.getFormattedDesc().contains("本校预计触达【学生】 1 人"));

        // 3. 租户 A 预估教师角色
        BroadcastEstimateVO estTeacherA = broadcastService.estimateAudience("role", "TEACHER");
        Assertions.assertEquals(1, estTeacherA.getEstimatedCount(), "租户 A 仅有 1 个教师");
        Assertions.assertTrue(estTeacherA.getFormattedDesc().contains("本校预计触达【教师】 1 人"));

        // 4. 切换至租户 B 预估全体受众
        mockLogin(TENANT_B, ADMIN_B, "TENANT_ADMIN", List.of("notice:broadcast:view"));
        BroadcastEstimateVO estAllB = broadcastService.estimateAudience("all", null);
        Assertions.assertEquals(2, estAllB.getEstimatedCount(), "租户 B 共有 2 个活跃成员");
        Assertions.assertTrue(estAllB.getFormattedDesc().contains("本校预计触达全体成员 2 人"));
    }
}
