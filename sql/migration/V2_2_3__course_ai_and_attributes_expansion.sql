-- ==========================================================
-- V2.2.3: 课程 AI 人设风格、学分学时与专属知识库字段扩充
-- ==========================================================

ALTER TABLE course 
  ADD COLUMN category VARCHAR(64) DEFAULT '计算机与软件' COMMENT '学科门类',
  ADD COLUMN credits DECIMAL(3,1) DEFAULT 3.0 COMMENT '学分',
  ADD COLUMN planned_hours INT DEFAULT 48 COMMENT '计划学时',
  ADD COLUMN knowledge_base_id BIGINT DEFAULT NULL COMMENT '关联专有知识库ID',
  ADD COLUMN ai_persona VARCHAR(32) DEFAULT 'socrates' COMMENT 'AI助教人设风格',
  ADD COLUMN welcome_message TEXT DEFAULT NULL COMMENT 'AI助教专属定制欢迎语';

UPDATE course SET 
  category = '计算机与软件',
  credits = 4.0,
  planned_hours = 64,
  ai_persona = 'socrates',
  welcome_message = '同学你好！我是《数据结构与算法》苏格拉底启发式 AI 助教。让我们通过层层深入的提问，共同探索算法底层逻辑！'
WHERE id = 101;

UPDATE course SET 
  category = '计算机与软件',
  credits = 3.5,
  planned_hours = 56,
  ai_persona = 'engineer',
  welcome_message = '嗨！我是《Java面向对象程序设计》实战导师。代码是运行出来的，遇到任何报错随时向我提问！'
WHERE id = 102;

UPDATE course SET 
  category = '通用高等数学',
  credits = 5.0,
  planned_hours = 80,
  ai_persona = 'academic',
  welcome_message = '同学你好！我是《高等数学（上）》学术助教。我将为你提供严密的概念辨析与定理推导。'
WHERE id = 103;
