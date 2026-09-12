-- Gate F E2E 隔离测试种子（幂等，可重复执行）
-- 用途：teacher2 独占课程 104 + 知识库 3，teacher(id=2) 无成员资格，用于验证数据隔离

USE edumind;

INSERT INTO sys_user (id, username, password, real_name, email, phone, avatar, status)
SELECT 5, 'teacher2', '$2b$10$DLVcEXn5RunOfqlY84u9S.nnXViwwLRheQgk0KIIpKr9y1im4mIOq',
       '李老师', 'teacher2@edumind.edu', '13800000005',
       'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png', 'ENABLE'
WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 5 OR username = 'teacher2');

INSERT INTO sys_user_role (user_id, role_id)
SELECT 5, 2
WHERE NOT EXISTS (SELECT 1 FROM sys_user_role WHERE user_id = 5 AND role_id = 2);

INSERT INTO course (id, title, code, teacher_id, semester, description, cover_image, status)
SELECT 104, 'Gate F 隔离测试课程', 'GATE104', 5, '2025秋',
       '仅 teacher2 可访问，用于 Gate F 数据隔离 E2E', NULL, 1
WHERE NOT EXISTS (SELECT 1 FROM course WHERE id = 104);

INSERT INTO course_member (course_id, user_id, member_role)
SELECT 104, 5, 'TEACHER'
WHERE NOT EXISTS (SELECT 1 FROM course_member WHERE course_id = 104 AND user_id = 5);

INSERT INTO knowledge_base (id, name, course_id, description, document_count, chunk_count, index_status, status)
SELECT 3, 'Gate F 隔离测试知识库', 104, 'teacher2 专属，teacher 不可访问', 0, 0, 'PENDING', 1
WHERE NOT EXISTS (SELECT 1 FROM knowledge_base WHERE id = 3);
