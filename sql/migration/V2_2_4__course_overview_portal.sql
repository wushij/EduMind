-- ==========================================================
-- V2.2.4: 课程概览门户 — 教学目标、公告、教学团队展示档案
-- ==========================================================

CREATE TABLE IF NOT EXISTS course_learning_objective (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id    BIGINT       NOT NULL DEFAULT 1 COMMENT '租户ID',
    course_id    BIGINT       NOT NULL COMMENT '课程ID',
    sort_order   INT          NOT NULL DEFAULT 1 COMMENT '排序 1-6',
    title        VARCHAR(80)  NOT NULL COMMENT '目标标题',
    description  VARCHAR(500) DEFAULT NULL COMMENT '目标说明',
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_clo_course (course_id),
    KEY idx_clo_tenant_course (tenant_id, course_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程教学目标';

CREATE TABLE IF NOT EXISTS course_announcement (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id       BIGINT       NOT NULL DEFAULT 1 COMMENT '租户ID',
    course_id       BIGINT       NOT NULL COMMENT '课程ID',
    title           VARCHAR(200) NOT NULL COMMENT '公告标题',
    content         TEXT         NOT NULL COMMENT '公告正文',
    pinned          TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否置顶',
    status          VARCHAR(20)  NOT NULL DEFAULT 'PUBLISHED' COMMENT 'PUBLISHED/DRAFT/WITHDRAWN',
    publish_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
    publisher_id    BIGINT       DEFAULT NULL COMMENT '发布人ID',
    publisher_name  VARCHAR(64)  DEFAULT NULL COMMENT '发布人姓名快照',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_ca_course_status (course_id, status, pinned, publish_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程公告';

CREATE TABLE IF NOT EXISTS course_instructor_profile (
    id            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id     BIGINT        NOT NULL DEFAULT 1 COMMENT '租户ID',
    course_id     BIGINT        NOT NULL COMMENT '课程ID',
    user_id       BIGINT        NOT NULL COMMENT '教师用户ID',
    intro         VARCHAR(1000) DEFAULT NULL COMMENT '本课程教师简介',
    office_hours  VARCHAR(200)  DEFAULT NULL COMMENT '答疑时间说明',
    sort_order    INT           NOT NULL DEFAULT 0 COMMENT '展示排序',
    is_primary    TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '是否主讲',
    create_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_cip_course_user (course_id, user_id),
    KEY idx_cip_course (course_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程教学团队展示档案';

INSERT INTO course_instructor_profile (tenant_id, course_id, user_id, is_primary, sort_order)
SELECT c.tenant_id, c.id, c.teacher_id, 1, 0
FROM course c
WHERE c.teacher_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM course_instructor_profile p
    WHERE p.course_id = c.id AND p.user_id = c.teacher_id
  );
