package com.edumind;

import com.edumind.common.constant.SecurityConstant;
import com.edumind.common.context.TenantContext;
import com.edumind.common.model.LoginUser;
import com.edumind.common.model.UserContext;
import com.edumind.course.dto.overview.CourseAnnouncementCreateDTO;
import com.edumind.course.dto.overview.CourseObjectiveItemDTO;
import com.edumind.course.dto.overview.CourseObjectivesSaveDTO;
import com.edumind.course.service.overview.CourseAnnouncementService;
import com.edumind.course.service.overview.CourseObjectiveService;
import com.edumind.course.service.overview.CourseOverviewService;
import com.edumind.course.vo.overview.CourseOverviewVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

@SpringBootTest(classes = EduMindApplication.class)
@ActiveProfiles("test")
public class CourseOverviewPortalIntegrationTest {

    private static final Long TENANT_ID = 1L;
    private static final Long COURSE_ID = 101L;
    /** init.sql 课程 101 的 teacher_id 与 TEACHER 成员均为用户 2 */
    private static final Long TEACHER_USER_ID = 2L;

    @Autowired
    private CourseOverviewService overviewService;

    @Autowired
    private CourseObjectiveService objectiveService;

    @Autowired
    private CourseAnnouncementService announcementService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        ensurePortalTables();
        cleanPortalData();
        TenantContext.setTenantId(TENANT_ID);
        LoginUser user = LoginUser.builder()
                .id(TEACHER_USER_ID)
                .username("teacher")
                .roles(List.of(SecurityConstant.ROLE_TEACHER))
                .permissions(List.of("course:view", "course:edit"))
                .build();
        UserContext.set(user);
    }

    @AfterEach
    void tearDown() {
        cleanPortalData();
        TenantContext.clear();
        UserContext.clear();
    }

    @Test
    void overview_shouldPersistObjectivesAndAnnouncements() {
        CourseObjectivesSaveDTO objectivesDto = new CourseObjectivesSaveDTO();
        CourseObjectiveItemDTO item = new CourseObjectiveItemDTO();
        item.setTitle("掌握核心概念");
        item.setDescription("能够准确表述课程核心知识点。");
        objectivesDto.setObjectives(List.of(item));
        objectiveService.saveAll(COURSE_ID, objectivesDto);

        CourseAnnouncementCreateDTO ann = new CourseAnnouncementCreateDTO();
        ann.setTitle("第一周学习安排");
        ann.setContent("请完成第一章预习。");
        ann.setPinned(true);
        var published = announcementService.create(COURSE_ID, ann);

        Integer notificationCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_notification WHERE type = 'COURSE' AND ref_id = ?",
                Integer.class,
                published.getId());
        Assertions.assertNotNull(notificationCount);
        Assertions.assertTrue(notificationCount >= 1, "发布公告应向课程成员推送站内通知");

        CourseOverviewVO overview = overviewService.getOverview(COURSE_ID);
        Assertions.assertNotNull(overview.getCourse());
        Assertions.assertEquals(1, overview.getObjectives().size());
        Assertions.assertFalse(overview.getAnnouncementsPreview().isEmpty());
        Assertions.assertTrue(overview.getEditable());
        Assertions.assertFalse(overview.getCapabilityTags().isEmpty());
    }

    private void cleanPortalData() {
        jdbcTemplate.update(
                "DELETE FROM sys_notification WHERE type = 'COURSE' AND ref_id IN (SELECT id FROM course_announcement WHERE course_id = ?)",
                COURSE_ID);
        jdbcTemplate.update("DELETE FROM course_learning_objective WHERE course_id = ?", COURSE_ID);
        jdbcTemplate.update("DELETE FROM course_announcement WHERE course_id = ?", COURSE_ID);
    }

    private void ensurePortalTables() {
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS course_learning_objective (
              id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
              tenant_id BIGINT NOT NULL DEFAULT 1,
              course_id BIGINT NOT NULL,
              sort_order INT NOT NULL DEFAULT 1,
              title VARCHAR(80) NOT NULL,
              description VARCHAR(500),
              create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
              update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
            )
            """);
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS course_announcement (
              id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
              tenant_id BIGINT NOT NULL DEFAULT 1,
              course_id BIGINT NOT NULL,
              title VARCHAR(200) NOT NULL,
              content TEXT NOT NULL,
              pinned TINYINT(1) NOT NULL DEFAULT 0,
              status VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED',
              publish_time DATETIME DEFAULT CURRENT_TIMESTAMP,
              publisher_id BIGINT,
              publisher_name VARCHAR(64),
              create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
              update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
            )
            """);
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS course_instructor_profile (
              id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
              tenant_id BIGINT NOT NULL DEFAULT 1,
              course_id BIGINT NOT NULL,
              user_id BIGINT NOT NULL,
              intro VARCHAR(1000),
              office_hours VARCHAR(200),
              sort_order INT NOT NULL DEFAULT 0,
              is_primary TINYINT(1) NOT NULL DEFAULT 0,
              create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
              update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
              UNIQUE KEY uk_cip_course_user (course_id, user_id)
            )
            """);
    }
}
