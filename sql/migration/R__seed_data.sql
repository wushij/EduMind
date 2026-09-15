-- =============================================================================
-- 智教云 · EduMind 基础种子数据（可重复执行，使用 INSERT IGNORE）
-- 文件：sql/migration/R__seed_data.sql
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. 系统角色与用户
-- 密码均为 admin123（BCrypt密文）
-- -----------------------------------------------------------------------------
INSERT IGNORE INTO sys_role (id, role_code, role_name, description) VALUES
(1, 'ADMIN',   '系统管理员', '平台系统全量管理权限'),
(2, 'TEACHER', '教师',       '教学管理、出题组卷与作业批改'),
(3, 'STUDENT', '学生',       '课程学习、在线测试与智能练习');

INSERT IGNORE INTO sys_user (id, username, password, real_name, email, phone, avatar, status) VALUES
(1, 'admin',     '$2b$10$DLVcEXn5RunOfqlY84u9S.nnXViwwLRheQgk0KIIpKr9y1im4mIOq', '系统管理员', NULL, NULL, 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png', 'ENABLE'),
(2, 'teacher',   '$2b$10$DLVcEXn5RunOfqlY84u9S.nnXViwwLRheQgk0KIIpKr9y1im4mIOq', '张老师',     NULL, NULL, 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png', 'ENABLE'),
(3, 'student',   '$2b$10$DLVcEXn5RunOfqlY84u9S.nnXViwwLRheQgk0KIIpKr9y1im4mIOq', '李同学',     NULL, NULL, 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png', 'ENABLE'),
(4, 'student2',  '$2b$10$DLVcEXn5RunOfqlY84u9S.nnXViwwLRheQgk0KIIpKr9y1im4mIOq', '王同学',     NULL, NULL, 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png', 'ENABLE');

INSERT IGNORE INTO sys_user_role (user_id, role_id) VALUES
(1, 1), (2, 2), (3, 3), (4, 3);

-- -----------------------------------------------------------------------------
-- 2. 细粒度权限配置
-- -----------------------------------------------------------------------------
INSERT IGNORE INTO sys_permission (id, permission_code, permission_name, parent_id) VALUES
(1,  'system:user:view',    '用户查看', 0),
(2,  'system:user:edit',    '用户编辑', 0),
(3,  'system:role:view',    '角色查看', 0),
(4,  'system:role:edit',    '角色编辑', 0),
(5,  'course:view',         '课程查看', 0),
(6,  'course:create',       '课程创建', 0),
(7,  'course:edit',         '课程编辑', 0),
(8,  'question:view',       '题目查看', 0),
(9,  'question:edit',       '题目编辑', 0),
(10, 'exam:view',           '试卷查看', 0),
(11, 'exam:edit',           '试卷编辑', 0),
(12, 'assignment:view',     '作业查看', 0),
(13, 'assignment:create',   '作业创建', 0),
(14, 'assignment:grade',    '作业批改', 0),
(15, 'knowledge:view',      '知识库查看', 0),
(16, 'knowledge:edit',      '知识库编辑', 0),
(17, 'ai:chat',             'AI对话', 0),
(18, 'ai:grading',          'AI批改', 0),
(19, 'ai:question',         'AI出题', 0),
(20, 'ai:exam',             'AI组卷', 0),
(21, 'analytics:view',      '学情分析查看', 0),
(22, 'resource:view',       '资源查看', 0),
(23, 'resource:upload',     '资源上传', 0),
(24, 'notice:view',         '通知查看', 0),
(31, 'ai:tool:use',         'Agent工具调用', 0),
(39, 'notice:broadcast:view', '广播推送查看', 0),
(40, 'notice:broadcast:send', '广播推送发送', 0),
(53, 'ai:memory:view',      '长期记忆查看', 0),
(54, 'ai:memory:manage',    '长期记忆管理', 0),
(71, 'course:delete',               '课程删除', 0),
(72, 'course:ai:use',               '课程 AI 使用', 0),
(73, 'ai:lesson:generate',          'AI 教案生成', 0),
(74, 'ai:summary:view',             'AI 课堂总结查看', 0),
(75, 'ai:recommendation:view',      'AI 推荐查看', 0),
(76, 'knowledge:create',            '知识库创建', 0),
(77, 'knowledge:delete',            '知识库删除', 0),
(78, 'knowledge:chunk:view',        '切片管理查看', 0),
(79, 'knowledge:vector:view',       '向量状态查看', 0),
(80, 'knowledge:graph:view',        '知识图谱查看', 0),
(81, 'question:create',             '题目创建', 0),
(82, 'question:delete',             '题目删除', 0),
(83, 'question:bank:view',          '题库查看', 0),
(84, 'learning:view',               '学习总览查看', 0),
(85, 'learning:task:view',          '学习任务查看', 0),
(86, 'learning:practice',           'AI 练习使用', 0),
(87, 'learning:wrong:view',         '错题本查看', 0),
(88, 'learning:report:view',        '学习报告查看', 0),
(89, 'learning:path:view',          '学习路径查看', 0),
(90, 'analytics:learning',          '学情分析查看', 0),
(91, 'analytics:mastery',           '知识点掌握分析', 0),
(92, 'analytics:wrong',             '错题分析查看', 0),
(93, 'analytics:ai-usage',          'AI 使用分析查看', 0),
(94, 'analytics:report',            '教学报告查看', 0),
(95, 'system:user:add',             '用户档案新增', 0),
(96, 'system:user:delete',          '用户档案删除', 0),
(97, 'system:menu:add',             '菜单管理新增', 0),
(98, 'system:menu:delete',          '菜单管理删除', 0),
(99, 'profile:view',                '个人资料查看', 0),
(100, 'profile:security',            '账号安全设置', 0),
(101, 'profile:preferences',         '偏好设置管理', 0);

-- 管理员具备所有权限
INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT 1, id FROM sys_permission;

-- 教师具备非系统管理权限
INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT 2, id FROM sys_permission WHERE permission_code NOT LIKE 'system:%';

-- 学生具备选课学习与自测权限
INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT 3, id FROM sys_permission WHERE permission_code IN (
    'course:view', 'assignment:view', 'exam:view', 'knowledge:view', 'ai:chat', 'resource:view', 'notice:view',
    'ai:memory:view', 'ai:memory:manage',
    'learning:view', 'learning:task:view', 'learning:practice', 'learning:wrong:view',
    'learning:report:view', 'learning:path:view',
    'profile:view', 'profile:security', 'profile:preferences'
);

-- -----------------------------------------------------------------------------
-- 3. 系统消息通知
-- -----------------------------------------------------------------------------
INSERT IGNORE INTO sys_notification (id, user_id, title, content, type, is_read, create_time) VALUES
(6, 1, '【系统欢迎】欢迎使用智教云 EduMind', '您已成功登录平台，可在顶部铃铛或个人中心查看教学、知识库与 AI 相关通知。', 'SYSTEM', 0, NOW()),
(7, 1, '【平台提示】消息通知中心已上线', '支持 WebSocket 实时推送、分类筛选与全部已读，请在个人中心体验完整功能。', 'SYSTEM', 0, NOW()),
(1, 3, '【作业截止提醒】第一单元链表作业即将截止', '您选修的《数据结构与算法》课程第一单元作业截止时间为今晚 23:59，请及时完成并提交作答。', 'ASSIGNMENT', 0, NOW()),
(2, 3, '【AI批改完成】单链表设计作业已完成评分', '张老师已确认您的作业批改成绩，综合得分 92 分，点击可查看详细 AI 知识盲点诊断与教师评语。', 'ASSIGNMENT', 1, NOW()),
(3, 3, '【系统升级】AI 智能助教与知识库升级通知', '平台已上线基于 RAG 的课程知识库向量问答增强系统，欢迎在课程详情中向 AI 助教提问！', 'SYSTEM', 0, NOW()),
(4, 2, '【批改待办】有 2 份学生作业待教师复核', '《数据结构与算法》课程中有 2 名学生已提交作业，AI 辅助批改已初步打分，请您查阅。', 'COURSE', 0, NOW()),
(5, 2, '【知识库提示】文档向量切片解析成功', '您上传的《数据结构第二章-线性表与链表深度解析.pdf》已完成切片与向量索引构建，已可供智能出题调用。', 'SYSTEM', 1, NOW());

-- -----------------------------------------------------------------------------
-- 4. 课程、章节、知识点与选课成员
-- -----------------------------------------------------------------------------
INSERT IGNORE INTO course (id, title, code, teacher_id, semester, description, cover_image, status) VALUES
(101, '数据结构与算法',     'CS201',   2, '2025秋', '计算机核心专业课，涵盖线性表、栈、队列、二叉树、图结构及常见排序检索算法设计。', 'https://images.unsplash.com/photo-1517694712202-14dd9538aa97?w=500', 1),
(102, 'Java面向对象程序设计', 'CS101',   2, '2025秋', '面向对象高级特性、集合框架、多线程并发与企业级工程架构实战。',       'https://images.unsplash.com/photo-1555066931-4365d14bab8c?w=500', 1),
(103, '高等数学（上）',     'MATH101', 2, '2025秋', '大学理工科公共核心基础，重点讲解极限论、导数与微分、不定积分与定积分应用。',   'https://images.unsplash.com/photo-1509228468518-180dd4864904?w=500', 1);

INSERT IGNORE INTO course_chapter (id, course_id, parent_id, title, sort_order) VALUES
(1,  101, 0,  '第一章 绪论与算法分析',      1),
(2,  101, 1,  '1.1 算法复杂度与渐进表示法',  1),
(3,  101, 0,  '第二章 线性表结构',          2),
(4,  101, 3,  '2.1 顺序存储与实现',          1),
(5,  101, 3,  '2.2 链式存储与单双向链表',    2),
(6,  101, 0,  '第三章 树与二叉树',          3),
(7,  102, 0,  '第一章 Java入门与基础语法',  1),
(8,  102, 7,  '1.1 变量作用域与数据类型',    1),
(9,  102, 0,  '第二章 面向对象核心思想',    2),
(10, 102, 9,  '2.1 封装、继承与多态机制',    1),
(11, 103, 0,  '第一章 函数与极限论',        1),
(12, 103, 11, '1.1 数列与函数极限计算',      1),
(13, 103, 0,  '第二章 导数与微分',          2),
(14, 103, 13, '2.1 复合函数与隐函数求导',    1);

INSERT IGNORE INTO course_knowledge_point (id, course_id, chapter_id, title, sort_order) VALUES
(10, 101, 2,  '时间与空间复杂度分析',            1),
(11, 101, 4,  '顺序表插入与删除时间开销',        2),
(12, 101, 5,  '单链表就地逆置算法',              3),
(13, 101, 6,  '二叉树先序中序后序遍历',          4),
(14, 102, 8,  '基本数据类型与包装类自动拆装箱',  1),
(15, 102, 10, '面向对象三大特征与多态运行时绑定', 2),
(16, 102, 10, 'ArrayList 与 LinkedList 源码剖析', 3),
(17, 103, 12, '等价无穷小代换及其应用条件',      1),
(18, 103, 12, '洛必达法则求未定式极限',          2),
(19, 103, 14, '复合函数链式求导法则',            3);

INSERT IGNORE INTO course_member (course_id, user_id, member_role) VALUES
(101, 2, 'TEACHER'), (101, 3, 'STUDENT'), (101, 4, 'STUDENT'),
(102, 2, 'TEACHER'), (102, 3, 'STUDENT'), (102, 4, 'STUDENT'),
(103, 2, 'TEACHER'), (103, 3, 'STUDENT'), (103, 4, 'STUDENT');

-- -----------------------------------------------------------------------------
-- 5. 题库、试题与选项
-- -----------------------------------------------------------------------------
INSERT IGNORE INTO question_bank (id, name, course_id, description, question_count, status, deleted) VALUES
(1, '数据结构核心真题库',   101, '涵盖全国统考408与期末高频真题，包括线性表、树与排序算法', 5, 1, 0),
(2, 'Java面向对象精选题集', 102, 'Java基础语法、面向对象、集合框架与异常处理典型题型',     3, 1, 0),
(3, '高等数学期末测试真题库', 103, '极限、连续、导数与微积分计算经典测试题',                 3, 1, 0);

INSERT IGNORE INTO edu_question (id, bank_id, course_id, knowledge_point_id, stem, type, options, answer, analysis, difficulty, score, status, deleted) VALUES
(1001, 3, 103, 17, '当 $x \\to 0$ 时，下列无穷小量中与 $x$ 等价的无穷小量是（ ）。', 'SINGLE_CHOICE',
 '[{"key":"A","content":"$\\\\sin 2x$"},{"key":"B","content":"$\\\\ln(1 + x)$"},{"key":"C","content":"$1 - \\\\cos x$"},{"key":"D","content":"$e^x - 1 - x$"}]',
 'B', '根据等价无穷小基本公式，当 $x \\to 0$ 时，$\\\\ln(1+x) \\sim x$；而 $\\\\sin 2x \\sim 2x$，$1-\\\\cos x \\sim \\\\frac{1}{2}x^2$。故正确答案为 B。', 3, 5, 1, 0),

(1002, 3, 103, 19, '设函数 $f(x) = \\ln(1 + x^2)$，则导数 $f\'(1)$ 的值为（ ）。', 'SINGLE_CHOICE',
 '[{"key":"A","content":"$\\\\frac{1}{2}$"},{"key":"B","content":"$1$"},{"key":"C","content":"$2$"},{"key":"D","content":"$\\\\ln 2$"}]',
 'B', '求复合函数导数：$f\'(x) = \\\\frac{1}{1 + x^2} \\\\cdot 2x = \\\\frac{2x}{1 + x^2}$，代入 $x = 1$ 得 $f\'(1) = \\\\frac{2}{2} = 1$。故选 B。', 2, 5, 1, 0),

(1003, 1, 101, 10, '已知一个栈的入栈序列为 1, 2, 3, 4, 5，则不可能得到的出栈序列是（ ）。', 'SINGLE_CHOICE',
 '[{"key":"A","content":"$4, 5, 3, 2, 1$"},{"key":"B","content":"$4, 3, 5, 1, 2$"},{"key":"C","content":"$1, 5, 4, 2, 3$"},{"key":"D","content":"$3, 4, 2, 1, 5$"}]',
 'B', '选项B中，当4、3出栈后，栈内剩余1、2，后压入5出栈后，栈顶应为2，不可能先出1再出2。故出栈序列 $4, 3, 5, 1, 2$ 不合法。', 3, 5, 1, 0),

(1004, 1, 101, 13, '下列关于平衡二叉树（AVL树）的叙述中，正确的有（ ）。', 'MULTIPLE_CHOICE',
 '[{"key":"A","content":"任意结点的左、右子树高度差绝对值不超过1"},{"key":"B","content":"查找操作的时间复杂度在最坏情况下为 $O(\\\\log n)$"},{"key":"C","content":"插入新结点引发失衡后，至多需要两次单旋转即可恢复平衡"},{"key":"D","content":"完全二叉树一定是AVL树"}]',
 'AB', '选项A为AVL树定义；选项B时间复杂度为对数级，正确；完全二叉树不一定是平衡查找树。故正确答案为 AB。', 4, 6, 1, 0),

(1005, 1, 101, 12, '在单链表中，增加头结点的目的是为了在首元结点之前插入新结点和删除首元结点的操作与其它结点的操作统一。', 'JUDGMENT',
 '[{"key":"T","content":"正确"},{"key":"F","content":"错误"}]',
 'T', '头结点的引入使得对首元结点的操作与后续结点的操作相同，无需单独维护头指针变量的重定向，统一了边界处理。', 2, 3, 1, 0),

(1006, 1, 101, 10, '请简要描述快速排序（QuickSort）的核心分治思想，并分析其最好、平均与最坏情况下的时间复杂度。', 'ESSAY',
 '[]',
 '分治思想：1. 选取基准元素pivot；2. 分区划分将小于等于pivot的放左侧，大于的放右侧；3. 递归排序左右两部分。时间复杂度：最好和平均均为O(nlogn)，最坏O(n^2)。',
 '考查快速排序的分治划分机制以及分区不平衡导致的退化现象。', 3, 10, 1, 0),

(1007, 2, 102, 16, '在 Java 集合框架中，关于 ArrayList 与 LinkedList 的特性描述，正确的是（ ）。', 'SINGLE_CHOICE',
 '[{"key":"A","content":"ArrayList 底层是动态数组，随机访问时间复杂度为 $O(1)$"},{"key":"B","content":"LinkedList 支持基于下标的 $O(1)$ 常数时间随机访问"},{"key":"C","content":"ArrayList 插入元素永远不需要复制数组"},{"key":"D","content":"LinkedList 占用内存比 ArrayList 更少"}]',
 'A', 'ArrayList底层是Object[]数组，支持下标随机访问；LinkedList为双向链表，查找需要遍历，且包含前后节点引用指针额外开销。故正确答案为 A。', 2, 5, 1, 0),

(1008, 2, 102, 14, 'Java 语言中，所有类的最终根父类是 ______。', 'BLANK',
 '[]',
 'java.lang.Object',
 'Java中任何未显式指定父类的类都隐式继承自 java.lang.Object。', 1, 4, 1, 0);

-- 试题选项明细
INSERT IGNORE INTO question_option (id, question_id, option_key, content, is_correct) VALUES
(1,  1001, 'A', '$\\sin 2x$', 0),
(2,  1001, 'B', '$\\ln(1 + x)$', 1),
(3,  1001, 'C', '$1 - \\cos x$', 0),
(4,  1001, 'D', '$e^x - 1 - x$', 0),
(5,  1002, 'A', '$\\frac{1}{2}$', 0),
(6,  1002, 'B', '$1$', 1),
(7,  1002, 'C', '$2$', 0),
(8,  1002, 'D', '$\\ln 2$', 0),
(9,  1003, 'A', '$4, 5, 3, 2, 1$', 0),
(10, 1003, 'B', '$4, 3, 5, 1, 2$', 1),
(11, 1003, 'C', '$1, 5, 4, 2, 3$', 0),
(12, 1003, 'D', '$3, 4, 2, 1, 5$', 0),
(13, 1004, 'A', '任意结点的左、右子树高度差绝对值不超过1', 1),
(14, 1004, 'B', '查找操作的时间复杂度在最坏情况下为 $O(\\log n)$', 1),
(15, 1004, 'C', '插入新结点引发失衡后，至多需要两次单旋转即可恢复平衡', 0),
(16, 1004, 'D', '完全二叉树一定是AVL树', 0),
(17, 1005, 'T', '正确', 1),
(18, 1005, 'F', '错误', 0),
(19, 1007, 'A', 'ArrayList 底层是动态数组，随机访问时间复杂度为 $O(1)$', 1),
(20, 1007, 'B', 'LinkedList 支持基于下标的 $O(1)$ 常数时间随机访问', 0),
(21, 1007, 'C', 'ArrayList 插入元素永远不需要复制数组', 0),
(22, 1007, 'D', 'LinkedList 占用内存比 ArrayList 更少', 0);

-- 题库与题目关联
INSERT IGNORE INTO question_bank_item (bank_id, question_id) VALUES
(1, 1003), (1, 1004), (1, 1005), (1, 1006),
(2, 1007), (2, 1008),
(3, 1001), (3, 1002);

-- -----------------------------------------------------------------------------
-- 6. 试卷与试题关联
-- -----------------------------------------------------------------------------
INSERT IGNORE INTO teaching_exam (id, course_id, title, total_score, pass_score, duration_minutes, start_time, end_time, status, deleted) VALUES
(501, 103, '2025秋季学期高等数学期中统一水平测试卷', 100, 60, 90,  '2025-10-15 09:00:00', '2025-10-15 10:30:00', 1, 0),
(502, 101, '数据结构与算法分析阶段性上机诊断试卷', 100, 60, 100, '2025-10-20 14:00:00', '2025-10-20 15:40:00', 1, 0);

INSERT IGNORE INTO exam_question (exam_id, question_id, score, sort_order) VALUES
(501, 1001, 10, 1),
(501, 1002, 10, 2),
(502, 1003, 10, 1),
(502, 1004, 15, 2),
(502, 1005, 10, 3),
(502, 1006, 25, 4);

-- -----------------------------------------------------------------------------
-- 7. 教学作业、提交与批改结果
-- -----------------------------------------------------------------------------
INSERT IGNORE INTO assignment (id, course_id, exam_id, title, description, deadline, status) VALUES
(201, 101, NULL, '第一单元：线性表与链表编程作业', '请完成单链表的基本操作及逆置算法设计，按要求提交核心复杂度分析。', '2025-10-25 23:59:59', 'PUBLISHED'),
(202, 103, 501,  '高数第三周同步随堂小测',       '函数极限计算与等价无穷小代换小测验，共2道题，限时45分钟。',         '2025-10-18 23:59:59', 'PUBLISHED');

INSERT IGNORE INTO assignment_submission (id, assignment_id, student_id, status, total_score, max_score, submit_time) VALUES
(301, 201, 3, 'GRADED',    92, 100, '2025-10-20 16:30:00'),
(302, 201, 4, 'SUBMITTED', NULL, 100, '2025-10-21 11:15:00');

INSERT IGNORE INTO submission_answer (id, submission_id, question_id, answer) VALUES
(401, 301, 1003, 'B'),
(402, 301, 1005, 'T'),
(403, 301, 1006, '快排基于分治思想：1. 选取基准值(pivot)；2. 将小于等于pivot的放左侧，大于的放右侧；3. 递归排序左右两部分。最好和平均时间复杂度为O(nlogn)，最坏情况当已有序时退化为O(n^2)。');

INSERT IGNORE INTO grading_result (id, submission_id, question_id, score, max_score, is_correct, ai_comment, teacher_comment, status) VALUES
(501, 301, 1003, 5,  5,  1, '作答完全正确，清晰理解了栈后进先出的约束条件。', '优秀', 'CONFIRMED'),
(502, 301, 1005, 3,  3,  1, '回答正确，头结点统一了空表和非空表的插入删除逻辑。', '完全正确', 'CONFIRMED'),
(503, 301, 1006, 9, 10,  2, '【AI批改分析】分治三个阶段描述准确，时间复杂度分析完备。失分点：未提及三数取中等优化避免退化的工程实践。', '思路很清晰，继续保持！', 'CONFIRMED');

-- -----------------------------------------------------------------------------
-- 8. 知识库、文档与切片文本
-- -----------------------------------------------------------------------------
INSERT IGNORE INTO knowledge_base (id, name, course_id, description, document_count, status) VALUES
(1, '数据结构与算法专业知识库', 101, '包含数据结构核心讲义、经典算法图解、大厂高频面试真题解析与课后作业参考。', 2, 1),
(2, 'Java面向对象程序设计知识库', 102, 'Java基础概念、JVM内存模型、多线程并发与常见框架最佳实践。', 1, 1);

INSERT IGNORE INTO knowledge_document (id, knowledge_base_id, file_name, file_type, file_size, object_key, parse_status, status) VALUES
(1, 1, '数据结构第二章-线性表与链表深度解析.pdf', 'PDF',  2457600, 'kb/1/cs201_ch2.pdf',        'SUCCESS', 1),
(2, 1, '常见经典树与图算法图解.pdf',             'PDF',  4194304, 'kb/1/cs201_tree_graph.pdf', 'SUCCESS', 1),
(3, 2, 'Java面向对象编程实战教程.docx',           'WORD', 1572864, 'kb/2/java_oop.docx',        'SUCCESS', 1);

INSERT IGNORE INTO knowledge_document_text (id, document_id, content) VALUES
(1, 1, '【线性表定义与特征】线性表是具有相同数据类型的n(n>=0)个数据元素的有限序列。其存储结构分为顺序存储与链式存储。顺序表物理地址连续，具备O(1)随机访问能力；链表通过指针域链接节点，适合频繁插入与删除。单链表头结点能统一首元结点与中间结点的操作边界。'),
(2, 2, '【二叉树核心性质与平衡树】非空二叉树上叶子结点数等于度为2的结点数加1。二叉平衡树(AVL)任何结点的左右子树高度差绝对值不超过1。当插入新结点引发失衡时，根据插入路径分为LL、RR、LR、RL四种形态，分别通过右旋、左旋或双旋在常数时间内恢复平衡。'),
(3, 3, '【面向对象三大特性剖析】封装隐藏了对象的内部细节，对外提供安全受控的公共访问入口；继承实现了代码复用与类型扩展；多态使得统一接口可以根据运行时的实际对象类型呈现不同的行为。多态三大必要条件：继承、方法重写、父类引用指向子类对象。');

-- -----------------------------------------------------------------------------
-- 9. 教学资源与课程资源关联
-- -----------------------------------------------------------------------------
INSERT IGNORE INTO teaching_resource (id, course_id, chapter_id, title, resource_type, file_url, description, status) VALUES
(1, 101, 1, '数据结构课件-第一章绪论.pdf',       'PDF',  '/resources/cs201-ch1.pdf',  '第一章绪论课件与教学目标', 1),
(2, 101, 3, '线性表算法设计与实验指导书.docx',   'WORD', '/resources/cs201-lab2.docx', '实验二线性表上机实践指导', 1),
(3, 102, 7, 'Java快速入门与JDK环境搭建指南.pptx', 'PPT',  '/resources/java-intro.pptx', '第一讲基础入门幻灯片',     1);

INSERT IGNORE INTO course_resource (id, course_id, resource_id, document_id, title, resource_type) VALUES
(1, 101, 1, NULL, '数据结构课件-第一章绪论.pdf',       'PDF'),
(2, 101, 2, NULL, '线性表算法设计与实验指导书.docx',   'WORD'),
(3, 101, NULL, 1, '数据结构第二章-线性表与链表深度解析.pdf', 'PDF'),
(4, 102, 3, NULL, 'Java快速入门与JDK环境搭建指南.pptx', 'PPT');

-- -----------------------------------------------------------------------------
-- 10. AI 工具广场元数据（14 项，对齐原型 §七）
-- -----------------------------------------------------------------------------
INSERT INTO ai_tool (id, name, description, detailed_intro, category, icon, model_id, route, execution_mode, tags, is_recommended, is_hot, use_count, status) VALUES
('tool_question_gen', 'AI 智能出题', '根据课程、章节和知识点智能生成高质量题目', '支持按章节与知识点勾选范围，配置题型、难度与题量后批量生成结构化试题，并可一键入库。', 'TEACHER', 'EditPen', 'deepseek-chat', '/ai/question/generate', 'ROUTE', '出题,教师,热门', 1, 1, 2436, 1),
('tool_exam_gen', 'AI 智能组卷', '按总分、题型比例与难度规则快速生成标准化试卷', '内置总分校验与题型配比引擎，支持预览换题、调分并保存为可复用试卷。', 'TEACHER', 'Document', 'deepseek-chat', '/ai/exam/generate', 'ROUTE', '组卷,教师', 1, 1, 1820, 1),
('tool_grading', 'AI 智能批改', '客观题秒级判分，主观题 AI 评分与评语生成', '支持作业提交后自动批改与教师复核改分，减轻期末阅卷压力。', 'TEACHER', 'Checked', 'deepseek-chat', '/ai/grading', 'ROUTE', '批改,教师', 1, 0, 956, 1),
('tool_lesson', 'AI 教案生成', '输入授课主题与学时，生成结构化教案与课堂设计', '覆盖教学目标、重难点、课堂互动与板书建议，辅助青年教师快速备课。', 'TEACHER', 'Notebook', 'deepseek-chat', '/ai/marketplace/v05/tool_lesson', 'V05_NOTICE', '教案,教师', 0, 0, 420, 1),
('tool_summary', 'AI 课程总结', '按章节或知识模块提炼核心要点与易错清单', '支持长文档与课件要点结构化摘要，生成考前复习精要。', 'TEACHER', 'DataAnalysis', 'deepseek-chat', '/ai/marketplace/v05/tool_summary', 'V05_NOTICE', '总结,知识提炼', 0, 0, 310, 1),
('tool_chat', 'AI 课程问答', '基于课程资料的上下文助教答疑（SSE 流式）', '在课程空间内多轮对话，支持 Markdown、公式与代码高亮渲染。', 'GENERAL', 'ChatDotRound', 'deepseek-chat', '/course/101/ai', 'ROUTE', '问答,助教,热门', 1, 1, 5200, 1),
('tool_wrong_analysis', 'AI 错题分析', '针对错题给出思路引导、错误归因与变式练习', '结合学生作答记录分析错因类型，并推荐巩固练习方向。', 'STUDENT', 'Warning', 'deepseek-chat', '/ai/marketplace/v05/tool_wrong_analysis', 'V05_NOTICE', '错题,学生', 0, 0, 680, 1),
('tool_knowledge_explain', 'AI 知识点讲解', '由浅入深讲解核心概念，支持苏格拉底式引导', '针对单个知识点提供类比、例题与追问，帮助学生建立直觉理解。', 'STUDENT', 'Reading', 'deepseek-chat', '/ai/marketplace/v05/tool_knowledge_explain', 'V05_NOTICE', '讲解,学生', 0, 0, 890, 1),
('tool_practice', 'AI 自适应刷题', '根据薄弱知识点智能生成阶梯练习', '分析近期学习数据，推送专项巩固题包与难度递进练习。', 'STUDENT', 'Reading', 'deepseek-chat', '/learning/recommendations', 'ROUTE', '练习,学生,推荐', 1, 0, 1680, 1),
('tool_learning_plan', 'AI 学习计划', '根据学情报告自动定制复习日程', '结合掌握度与考试节点生成周计划与每日任务清单。', 'STUDENT', 'Calendar', 'deepseek-chat', '/ai/marketplace/v05/tool_learning_plan', 'V05_NOTICE', '计划,学生', 0, 0, 540, 1),
('tool_ppt', 'AI PPT 生成', '根据大纲快速生成课件骨架与讲稿要点', '输出章节页结构与演讲备注，辅助课件制作。', 'TEACHER', 'Monitor', 'deepseek-chat', '/ai/marketplace/v05/tool_ppt', 'V05_NOTICE', 'PPT,教师', 0, 0, 260, 1),
('tool_polish', 'AI 教学文本润色', '优化题干表述，消除歧义与语病', '面向试题、教案与通知类文本提供学术化润色建议。', 'GENERAL', 'EditPen', 'deepseek-chat', '/ai/marketplace/v05/tool_polish', 'V05_NOTICE', '润色,通用', 0, 0, 380, 1),
('tool_translate', 'AI 双语专业翻译', '中英计算机与专业课术语精准对照翻译', '保持术语一致性，适合双语课件与论文摘要翻译。', 'GENERAL', 'Connection', 'deepseek-chat', '/ai/marketplace/v05/tool_translate', 'V05_NOTICE', '翻译,通用', 0, 0, 450, 1),
('tool_resource_rec', 'AI 资源推荐', '基于课程与章节推荐关联课件与视频资源', '在学习中心展示与当前课程强关联的习题与教学资源。', 'STUDENT', 'Promotion', 'deepseek-chat', '/learning/recommendations', 'ROUTE', '推荐,学生', 1, 0, 1320, 1)
ON DUPLICATE KEY UPDATE
  name = VALUES(name),
  description = VALUES(description),
  detailed_intro = VALUES(detailed_intro),
  category = VALUES(category),
  icon = VALUES(icon),
  model_id = VALUES(model_id),
  route = VALUES(route),
  execution_mode = VALUES(execution_mode),
  tags = VALUES(tags),
  is_recommended = VALUES(is_recommended),
  is_hot = VALUES(is_hot),
  use_count = VALUES(use_count),
  status = VALUES(status);

-- -----------------------------------------------------------------------------
-- 11. AI 示例会话与调用日志
-- -----------------------------------------------------------------------------
INSERT IGNORE INTO ai_conversation (id, user_id, course_id, title, message_count, total_tokens, deleted) VALUES
('conv_teacher_001', 2, 101, '数据结构期中试卷出题咨询', 2, 850, 0),
('conv_student_001', 3, 101, '关于单链表逆置算法的疑问解答', 2, 620, 0);

INSERT IGNORE INTO ai_message (id, conversation_id, role, content, token_count) VALUES
('msg_t_01', 'conv_teacher_001', 'user', '请根据数据结构第二章线性表知识点，帮我生成3道考察链表边界处理的单选题，难度中等。', 80),
('msg_t_02', 'conv_teacher_001', 'assistant', '好的张老师！已为您基于《数据结构》第二章生成 3 道考察链表边界指针处理的选择题，已自动匹配知识点【单链表就地逆置】与【顺序表边界条件】，请前往试题库查看或一键导入组卷。', 770),
('msg_s_01', 'conv_student_001', 'user', '助教你好，请问在做单链表就地逆置时，为什么必须先用临时指针保存 next 节点？', 60),
('msg_s_02', 'conv_student_001', 'assistant', '同学你好！在单链表就地逆置时，当我们执行 `curr->next = prev;` 反转当前节点的指针指向后，原有的后续节点链条就会断开。如果不提前使用 `temp = curr->next;` 记录后续节点地址，将无法继续遍历剩余链表，造成链表丢失（内存泄漏或无法循环）。', 560);

INSERT IGNORE INTO ai_call_log (id, user_id, model, prompt_tokens, completion_tokens, latency_ms, scene) VALUES
(1, 2, 'deepseek-chat', 320, 530, 1250, 'QUESTION_GEN'),
(2, 3, 'deepseek-chat', 180, 440, 890,  'AI_CHAT');

-- -----------------------------------------------------------------------------
-- 12. 每日学情统计快照（近 7 天历史趋势数据，供仪表盘与大屏图表渲染）
-- -----------------------------------------------------------------------------
INSERT IGNORE INTO statistics_daily_snapshot (id, stat_date, course_id, active_student_count, total_ai_conversations, total_tokens_consumed, avg_score) VALUES
(1, DATE_SUB(CURDATE(), INTERVAL 6 DAY), 101, 45, 120, 85000,  84.5),
(2, DATE_SUB(CURDATE(), INTERVAL 5 DAY), 101, 52, 145, 98000,  85.0),
(3, DATE_SUB(CURDATE(), INTERVAL 4 DAY), 101, 58, 160, 112000, 86.2),
(4, DATE_SUB(CURDATE(), INTERVAL 3 DAY), 101, 64, 188, 135000, 87.1),
(5, DATE_SUB(CURDATE(), INTERVAL 2 DAY), 101, 70, 210, 158000, 86.8),
(6, DATE_SUB(CURDATE(), INTERVAL 1 DAY), 101, 78, 235, 182000, 88.0),
(7, CURDATE(),                           101, 85, 260, 205000, 88.5);
