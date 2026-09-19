-- 题库演示数据收敛为 3 道典型题（1001 高数单选 / 1003 栈单选 / 1007 Java 单选）
-- 同时清理误入库 AI 题干与其它冗余题目，便于本地/测试环境一键恢复干净题库。

-- 1) 脏题干（与 V2_5_3 一致，可重复执行）
UPDATE edu_question
SET deleted = 1,
    update_time = NOW()
WHERE deleted = 0
  AND (
    stem LIKE 'Mock 题目%'
        OR stem LIKE '%你是一位专业的教学出题助手%'
        OR (stem LIKE '%JSON 格式%' AND stem LIKE '%questions%')
        OR (stem LIKE '%课程ID=%' AND stem LIKE '%知识点=%' AND stem LIKE '%题型=%')
  );

-- 2) 关联表：移除非保留题目引用
DELETE FROM grading_result
WHERE question_id NOT IN (1001, 1003, 1007);

DELETE FROM submission_answer
WHERE question_id NOT IN (1001, 1003, 1007);

DELETE FROM exam_question
WHERE question_id NOT IN (1001, 1003, 1007);

DELETE FROM question_bank_item
WHERE question_id NOT IN (1001, 1003, 1007);

DELETE FROM question_option
WHERE question_id NOT IN (1001, 1003, 1007);

DELETE FROM wrong_question_record
WHERE question_id NOT IN (1001, 1003, 1007);

-- 3) 逻辑删除除 3 道演示题外的全部题目（含 AI 误入库、历史种子 1002/1004/1005/1006/1008 等）
UPDATE edu_question
SET deleted = 1,
    update_time = NOW()
WHERE deleted = 0
  AND id NOT IN (1001, 1003, 1007);

-- 4) 确保 3 道演示题处于可用状态
UPDATE edu_question
SET deleted = 0,
    status  = 1,
    update_time = NOW()
WHERE id IN (1001, 1003, 1007);

-- 5) 若演示题被物理删除或从未初始化，补回最小种子（INSERT IGNORE 幂等）
INSERT IGNORE INTO edu_question (id, bank_id, course_id, knowledge_point_id, stem, type, options, answer, analysis, difficulty, score, status, deleted) VALUES
(1001, 3, 103, 17, '当 $x \\to 0$ 时，下列无穷小量中与 $x$ 等价的无穷小量是（ ）。', 'SINGLE_CHOICE',
 '[{"key":"A","content":"$\\\\sin 2x$"},{"key":"B","content":"$\\\\ln(1 + x)$"},{"key":"C","content":"$1 - \\\\cos x$"},{"key":"D","content":"$e^x - 1 - x$"}]',
 'B', '根据等价无穷小基本公式，当 $x \\to 0$ 时，$\\\\ln(1+x) \\sim x$；而 $\\\\sin 2x \\sim 2x$，$1-\\\\cos x \\sim \\\\frac{1}{2}x^2$。故正确答案为 B。', 3, 5, 1, 0),
(1003, 1, 101, 10, '已知一个栈的入栈序列为 1, 2, 3, 4, 5，则不可能得到的出栈序列是（ ）。', 'SINGLE_CHOICE',
 '[{"key":"A","content":"$4, 5, 3, 2, 1$"},{"key":"B","content":"$4, 3, 5, 1, 2$"},{"key":"C","content":"$1, 5, 4, 2, 3$"},{"key":"D","content":"$3, 4, 2, 1, 5$"}]',
 'B', '选项B中，当4、3出栈后，栈内剩余1、2，后压入5出栈后，栈顶应为2，不可能先出1再出2。故出栈序列 $4, 3, 5, 1, 2$ 不合法。', 3, 5, 1, 0),
(1007, 2, 102, 16, '在 Java 集合框架中，关于 ArrayList 与 LinkedList 的特性描述，正确的是（ ）。', 'SINGLE_CHOICE',
 '[{"key":"A","content":"ArrayList 底层是动态数组，随机访问时间复杂度为 $O(1)$"},{"key":"B","content":"LinkedList 支持基于下标的 $O(1)$ 常数时间随机访问"},{"key":"C","content":"ArrayList 插入元素永远不需要复制数组"},{"key":"D","content":"LinkedList 占用内存比 ArrayList 更少"}]',
 'A', 'ArrayList底层是Object[]数组，支持下标随机访问；LinkedList为双向链表，查找需要遍历，且包含前后节点引用指针额外开销。故正确答案为 A。', 2, 5, 1, 0);

INSERT IGNORE INTO question_option (id, question_id, option_key, content, is_correct) VALUES
(1,  1001, 'A', '$\\sin 2x$', 0),
(2,  1001, 'B', '$\\ln(1 + x)$', 1),
(3,  1001, 'C', '$1 - \\cos x$', 0),
(4,  1001, 'D', '$e^x - 1 - x$', 0),
(9,  1003, 'A', '$4, 5, 3, 2, 1$', 0),
(10, 1003, 'B', '$4, 3, 5, 1, 2$', 1),
(11, 1003, 'C', '$1, 5, 4, 2, 3$', 0),
(12, 1003, 'D', '$3, 4, 2, 1, 5$', 0),
(19, 1007, 'A', 'ArrayList 底层是动态数组，随机访问时间复杂度为 $O(1)$', 1),
(20, 1007, 'B', 'LinkedList 支持基于下标的 $O(1)$ 常数时间随机访问', 0),
(21, 1007, 'C', 'ArrayList 插入元素永远不需要复制数组', 0),
(22, 1007, 'D', 'LinkedList 占用内存比 ArrayList 更少', 0);

INSERT IGNORE INTO question_bank_item (bank_id, question_id) VALUES
(1, 1003),
(2, 1007),
(3, 1001);

INSERT IGNORE INTO exam_question (exam_id, question_id, score, sort_order) VALUES
(501, 1001, 10, 1),
(502, 1003, 10, 1);

UPDATE question_bank
SET question_count = 1,
    update_time    = NOW()
WHERE id IN (1, 2, 3)
  AND deleted = 0;
