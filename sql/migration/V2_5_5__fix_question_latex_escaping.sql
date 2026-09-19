-- =============================================================================
-- EduMind 数据库版本升级：V2_5_5 修复题库公式多重转义导致的 LaTeX 排版换行与公式破损问题
-- =============================================================================

-- 1. 修复 1001 题（高数等价无穷小演示单选）
UPDATE edu_question
SET analysis = '根据等价无穷小基本公式，当 $x \\to 0$ 时，\\ln(1+x) \\sim x；而 \\sin 2x \\sim 2x，$1-\\cos x \\sim \\frac{1}{2}x^2$。故正确答案为 B。',
    options = '[{"key":"A","content":"$\\sin 2x$"},{"key":"B","content":"$\\ln(1 + x)$"},{"key":"C","content":"$1 - \\cos x$"},{"key":"D","content":"$e^x - 1 - x$"}]'
WHERE id = 1001;

-- 2. 修复 1002 题（高数复合函数求导）
UPDATE edu_question
SET analysis = '求复合函数导数：$f\'(x) = \\frac{1}{1 + x^2} \\cdot 2x = \\frac{2x}{1 + x^2}$，代入 $x = 1$ 得 $f\'(1) = \\frac{2}{2} = 1$。故选 B。',
    options = '[{"key":"A","content":"$\\frac{1}{2}$"},{"key":"B","content":"$1$"},{"key":"C","content":"$2$"},{"key":"D","content":"$\\ln 2$"}]'
WHERE id = 1002;

-- 3. 通用清理历史数据中因重复转义遗留的双反斜杠 LaTeX 关键字
UPDATE edu_question
SET analysis = REPLACE(REPLACE(REPLACE(REPLACE(analysis, '\\\\ln', '\\ln'), '\\\\sin', '\\sin'), '\\\\cos', '\\cos'), '\\\\frac', '\\frac')
WHERE analysis LIKE '%\\\\\\\\%';

UPDATE edu_question
SET analysis = REPLACE(analysis, '\\\\cdot', '\\cdot')
WHERE analysis LIKE '%\\\\\\\\cdot%';
