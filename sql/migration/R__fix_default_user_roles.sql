-- Fix default demo account role/permission bindings (idempotent)
-- Usage: mysql -u root -p edumind < sql/migration/R__fix_default_user_roles.sql

INSERT IGNORE INTO sys_role (id, role_code, role_name, description) VALUES
(1, 'ADMIN',   'Admin',   'Full platform access'),
(2, 'TEACHER', 'Teacher', 'Teaching and grading access'),
(3, 'STUDENT', 'Student', 'Learning access');

INSERT IGNORE INTO sys_user (id, username, password, real_name, email, phone, avatar, status) VALUES
(1, 'admin',    '$2b$10$DLVcEXn5RunOfqlY84u9S.nnXViwwLRheQgk0KIIpKr9y1im4mIOq', 'Admin',   NULL, NULL, 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png', 'ENABLE'),
(2, 'teacher',  '$2b$10$DLVcEXn5RunOfqlY84u9S.nnXViwwLRheQgk0KIIpKr9y1im4mIOq', 'Teacher', NULL, NULL, 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png', 'ENABLE'),
(3, 'student',  '$2b$10$DLVcEXn5RunOfqlY84u9S.nnXViwwLRheQgk0KIIpKr9y1im4mIOq', 'Student', NULL, NULL, 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png', 'ENABLE'),
(4, 'student2', '$2b$10$DLVcEXn5RunOfqlY84u9S.nnXViwwLRheQgk0KIIpKr9y1im4mIOq', 'Student2',NULL, NULL, 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png', 'ENABLE');

INSERT IGNORE INTO sys_user_role (user_id, role_id) VALUES
(1, 1),
(2, 2),
(3, 3),
(4, 3);

INSERT INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id
FROM sys_user u
JOIN sys_role r ON r.role_code = 'ADMIN'
WHERE u.username = 'admin'
  AND NOT EXISTS (
      SELECT 1 FROM sys_user_role ur
      WHERE ur.user_id = u.id AND ur.role_id = r.id
  );

INSERT INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id
FROM sys_user u
JOIN sys_role r ON r.role_code = 'TEACHER'
WHERE u.username = 'teacher'
  AND NOT EXISTS (
      SELECT 1 FROM sys_user_role ur
      WHERE ur.user_id = u.id AND ur.role_id = r.id
  );

INSERT INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id
FROM sys_user u
JOIN sys_role r ON r.role_code = 'STUDENT'
WHERE u.username IN ('student', 'student2')
  AND NOT EXISTS (
      SELECT 1 FROM sys_user_role ur
      WHERE ur.user_id = u.id AND ur.role_id = r.id
  );

INSERT IGNORE INTO sys_permission (id, permission_code, permission_name, parent_id) VALUES
(1,  'system:user:view',    'system user view', 0),
(2,  'system:user:edit',    'system user edit', 0),
(3,  'system:role:view',    'system role view', 0),
(4,  'system:role:edit',    'system role edit', 0),
(5,  'course:view',         'course view', 0),
(6,  'course:create',       'course create', 0),
(7,  'course:edit',         'course edit', 0),
(8,  'question:view',       'question view', 0),
(9,  'question:edit',       'question edit', 0),
(10, 'exam:view',           'exam view', 0),
(11, 'exam:edit',           'exam edit', 0),
(12, 'assignment:view',     'assignment view', 0),
(13, 'assignment:create',   'assignment create', 0),
(14, 'assignment:grade',    'assignment grade', 0),
(15, 'knowledge:view',      'knowledge view', 0),
(16, 'knowledge:edit',      'knowledge edit', 0),
(17, 'ai:chat',             'ai chat', 0),
(18, 'ai:grading',          'ai grading', 0),
(19, 'ai:question',         'ai question', 0),
(20, 'ai:exam',             'ai exam', 0),
(21, 'analytics:view',      'analytics view', 0),
(22, 'resource:view',       'resource view', 0),
(23, 'resource:upload',     'resource upload', 0),
(24, 'notice:view',         'notice view', 0),
(31, 'ai:tool:use',         'ai tool use', 0);

INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT 1, id FROM sys_permission;

INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT 2, id FROM sys_permission WHERE permission_code NOT LIKE 'system:%';

INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT 3, id FROM sys_permission WHERE permission_code IN (
    'course:view', 'assignment:view', 'exam:view', 'knowledge:view', 'ai:chat', 'resource:view', 'notice:view'
);
