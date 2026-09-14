# 智教云 · EduMind 数据库脚本中心

本项目所有数据库脚本统一维护在根目录 `sql/` 下。**后端启动不会自动改库**，需由你在 MySQL 中手动执行。

---

## 目录结构

```text
sql/
├── init.sql                                    # 安全初始化（仅 IF NOT EXISTS 建表 + INSERT IGNORE 种子，**不 DROP**）
├── migration/                                  # 增量版本脚本（每个大版本一个文件）
│   ├── V0_1_0__mvp_core.sql                    # V0.1 MVP 核心（用户/课程/AI/题目/试卷/会话）
│   ├── V0_2_0__mvp_expansion.sql               # V0.2 MVP 扩展（题库/作业/文档/RBAC/通知/工具广场）
│   ├── V0_5_0__product_enhancement.sql         # V0.5 产品增强（Chunk/向量/RAG/Prompt/配额/权限）
│   ├── V0_5_1__user_preferences.sql            # V0.5.1 用户偏好设置
│   ├── V1_0_0__intelligent_hub.sql             # V1.0 智能教学中枢（学情/图谱/Gateway/Agent）
│   ├── V1_0_1__agent_tool_permission.sql       # V1.0.1 Agent Tool 调用权限
│   ├── V1_1_0__ai_call_log_course_id.sql       # V1.1.0 AI 审计日志增加 course_id
│   ├── V1_1_1__course_statistics_job.sql       # V1.1.1 课程学情日聚合表结构
│   ├── V1_2_0__ai_model_config_ops.sql         # V1.2.0 AI 模型运维字段与 Embedding 默认配置
│   ├── V1_2_1__ai_message_reasoning.sql        # V1.2.1 AI 消息思考链 reasoning_content
│   ├── V2_0_0__multi_tenant_core.sql           # V2.0 多租户/组织/记忆/OCR/导出/国密/干预
│   ├── V2_0_1__system_config_expansion.sql   # V2.0.1 系统配置十二分组 + 短信/邮件审计表
│   ├── V2_0_2__legacy_core_tenant_id.sql       # V2.0.2 核心业务表 tenant_id 列扩展
│   ├── V2_0_3__notification_ref_id.sql         # V2.0.3 通知关联业务 ID + 已读索引
│   ├── V2_0_4__notification_broadcast.sql      # V2.0.4 通知优先级 + 广播任务表
│   ├── V2_0_5__notification_broadcast_permissions.sql # V2.0.5 广播推送权限种子
│   ├── V2_0_6__system_and_ai_compute_permissions.sql  # V2.0.6 系统管理/AI智算权限种子
│   ├── V2_0_7__memory_retention.sql            # V2.0.7 长期记忆留存周期 + 记忆权限
│   ├── V2_0_8__ai_model_max_tokens_8192.sql   # V2.0.8 Chat 模型默认 max_tokens 提升至 8192
│   ├── V2_0_9__course_rag_prompt_templates.sql # V2.0.9 Prompt 表扩展 + 课程 RAG 提示词资产
│   ├── V2_0_10__notification_tenant.sql        # V2.0.10 通知广播多租户隔离与权限增强 (Gate I5)
│   ├── V2_0_11__ai_call_log_conversation_id.sql # V2.0.11 ai_call_log 增加 conversation_id
│   ├── V2_0_12__knowledge_ocr_permissions.sql  # V2.0.12 知识库 OCR 识别与校对使用权限 (Gate I6)
│   ├── V2_0_13__chat_rag_prompt_and_tokens.sql # V2.0.13 课程轻量版 RAG Prompt 与 8000 Token 上限
│   ├── V2_0_14__exam_rag_prompt_template.sql   # V2.0.14 智能命题（RAG）通用核心提示词模板 (EXAM_RAG_GENERAL)
│   ├── V2_0_15__grading_rag_prompt_template.sql # V2.0.15 智能批改（RAG）通用核心提示词模板 (GRADING_RAG_GENERAL)
│   ├── V2_0_16__lesson_prep_rag_prompt_template.sql # V2.0.16 教案备课（RAG）通用核心提示词模板 (LESSON_PREP_RAG_GENERAL)
│   ├── V2_0_17__export_task_async.sql          # V2.0.17 试卷排版导出异步化与下载鉴权 (Gate I7)
│   ├── R__gate_f_e2e_seed.sql                  # Gate F 隔离测试种子（teacher2 + course104）
│   ├── R__gate_g_e2e_seed.sql                  # Gate G E2E 种子（掌握度/图谱/错题，幂等）
│   ├── R__gate_h_e2e_seed.sql                  # Gate H E2E 种子（course_statistics/ai_call_log，幂等）
│   ├── R__seed_data.sql                        # 种子数据（可重复执行，注意幂等）
│   ├── R__seed_chat_thinking_prompt.sql        # 课程 AI Prompt 增加简要思考结构（幂等）
│   ├── R__seed_tenant_roles.sql                # 租户/院系管理员角色与组织权限（幂等，旧库升级用）
│   ├── R__seed_member_org.sql                  # 组织成员分配演示种子（幂等）
│   ├── R__fix_default_user_roles.sql           # 修复默认账号角色绑定（幂等）
│   └── R__clear_default_user_contact.sql       # 清除演示账号种子邮箱/手机（幂等）
└── rollback/
    └── V1_1_rollback.sql              # V1.1 回滚（course_statistics / course_id）
```

---

## 使用方式

### 方式一：安全初始化（仅全新空库）

**仅当 `edumind` 库内没有任何表（0 张表）时** 可执行：

```bash
mysql -u root -p < sql/init.sql
```

执行时会输出安全提示。**库中已有表会立即报错终止，无法绕过。**

> 已有业务数据的库 **禁止** 执行 `init.sql`；补表、改结构、版本升级请走 `sql/migration/V*.sql`（执行前 `mysqldump` 备份）。

`init.sql` 已包含 **V0.1 ~ V2.0.12** 与 **V1.2.x** 的全部 `V*.sql` 迁移最终状态（含课程 RAG 提示词、通知多租户、OCR 权限等），**全新空库跑 init 后无需再跑 migration**（Gate E2E 可选种子除外）。

### 方式二：按版本增量迁移（已有空库分步升级）

在已创建 `edumind` 数据库的前提下，**每个大版本只执行一个脚本**：

```text
1. V0_1_0__mvp_core.sql
2. V0_2_0__mvp_expansion.sql
3. V0_5_0__product_enhancement.sql
4. V0_5_1__user_preferences.sql
5. V1_0_0__intelligent_hub.sql
6. V1_0_1__agent_tool_permission.sql
7. V1_1_0__ai_call_log_course_id.sql
8. V1_1_1__course_statistics_job.sql
9. V2_0_0__multi_tenant_core.sql
10. V1_2_0__ai_model_config_ops.sql
11. V1_2_1__ai_message_reasoning.sql
12. V2_0_1__system_config_expansion.sql
13. V2_0_2__legacy_core_tenant_id.sql
14. V2_0_3__notification_ref_id.sql
15. V2_0_4__notification_broadcast.sql
16. V2_0_5__notification_broadcast_permissions.sql
17. V2_0_6__system_and_ai_compute_permissions.sql
18. V2_0_7__memory_retention.sql
19. V2_0_8__ai_model_max_tokens_8192.sql
20. V2_0_9__course_rag_prompt_templates.sql
21. V2_0_10__notification_tenant.sql
22. V2_0_11__ai_call_log_conversation_id.sql
23. V2_0_12__knowledge_ocr_permissions.sql
24. V2_0_13__chat_rag_prompt_and_tokens.sql
25. V2_0_14__exam_rag_prompt_template.sql
26. V2_0_15__grading_rag_prompt_template.sql
27. V2_0_16__lesson_prep_rag_prompt_template.sql
28. R__seed_data.sql          # 可选，补充演示种子数据
29. R__seed_tenant_roles.sql  # 旧库升级：租户/院系管理员角色与组织权限
```

示例：

```bash
mysql -u root -p edumind < sql/migration/V0_1_0__mvp_core.sql
mysql -u root -p edumind < sql/migration/V0_2_0__mvp_expansion.sql
mysql -u root -p edumind < sql/migration/V0_5_0__product_enhancement.sql
mysql -u root -p edumind < sql/migration/V0_5_1__user_preferences.sql
mysql -u root -p edumind < sql/migration/V1_0_0__intelligent_hub.sql
mysql -u root -p edumind < sql/migration/V1_0_1__agent_tool_permission.sql
mysql -u root -p edumind < sql/migration/V1_1_0__ai_call_log_course_id.sql
mysql -u root -p edumind < sql/migration/V1_1_1__course_statistics_job.sql
mysql -u root -p edumind < sql/migration/V2_0_0__multi_tenant_core.sql
mysql -u root -p edumind < sql/migration/V1_2_0__ai_model_config_ops.sql
mysql -u root -p edumind < sql/migration/V1_2_1__ai_message_reasoning.sql
mysql -u root -p edumind < sql/migration/V2_0_1__system_config_expansion.sql
mysql -u root -p edumind < sql/migration/V2_0_2__legacy_core_tenant_id.sql
mysql -u root -p edumind < sql/migration/V2_0_3__notification_ref_id.sql
mysql -u root -p edumind < sql/migration/V2_0_4__notification_broadcast.sql
mysql -u root -p edumind < sql/migration/V2_0_5__notification_broadcast_permissions.sql
mysql -u root -p edumind < sql/migration/V2_0_6__system_and_ai_compute_permissions.sql
mysql -u root -p edumind < sql/migration/V2_0_7__memory_retention.sql
mysql -u root -p edumind < sql/migration/V2_0_8__ai_model_max_tokens_8192.sql
mysql -u root -p edumind < sql/migration/V2_0_9__course_rag_prompt_templates.sql
mysql -u root -p edumind < sql/migration/V2_0_10__notification_tenant.sql
mysql -u root -p edumind < sql/migration/V2_0_11__ai_call_log_conversation_id.sql
mysql -u root -p edumind < sql/migration/V2_0_12__knowledge_ocr_permissions.sql
mysql -u root -p edumind < sql/migration/V2_0_13__chat_rag_prompt_and_tokens.sql
mysql -u root -p edumind < sql/migration/V2_0_14__exam_rag_prompt_template.sql
mysql -u root -p edumind < sql/migration/V2_0_15__grading_rag_prompt_template.sql
mysql -u root -p edumind < sql/migration/R__seed_data.sql
mysql -u root -p edumind < sql/migration/R__seed_tenant_roles.sql
```

> **注意：** 若已执行过 `init.sql`，通常无需再跑 migration 脚本，避免重复建表。两种方式二选一即可。  
> 若库是 V2.0.2 之前建的旧库，需额外执行 `R__seed_tenant_roles.sql` 补齐租户 RBAC。

### 删表与结构变更规则

| 脚本 | 是否允许 DROP | 用途 |
|------|---------------|------|
| `sql/init.sql` | **禁止** | **仅空库**可执行；建表 + 种子（不 DROP） |
| `sql/migration/V*.sql` | **允许** | 版本升级、ALTER、按需 DROP |
| `sql/rollback/*.sql` | **允许** | 回滚特定版本 |

### 已有库升级（按目标版本单脚本执行）

若库是早期版本建的，只需执行**尚未执行过的版本**对应脚本：

| 目标版本 | 迁移脚本 | 说明 |
|:---|:---|:---|
| V0.1 MVP 核心 | `V0_1_0__mvp_core.sql` | 用户、课程、AI、题目、试卷、会话 |
| V0.2 MVP 扩展 | `V0_2_0__mvp_expansion.sql` | 题库、作业批改、知识库文档、RBAC、通知、AI 工具广场 |
| V0.5 产品增强 | `V0_5_0__product_enhancement.sql` | Chunk、向量索引、RAG、Prompt 治理、配额、权限 |
| V0.5.1 用户偏好 | `V0_5_1__user_preferences.sql` | 用户偏好设置 `sys_user_preference` |
| V1.0 智能教学中枢 | `V1_0_0__intelligent_hub.sql` | 学情/掌握度/图谱关系/AI Gateway/Agent |
| V1.0.1 Agent 权限 | `V1_0_1__agent_tool_permission.sql` | `ai:tool:use` 权限种子 |
| V1.1.0 AI 审计维度 | `V1_1_0__ai_call_log_course_id.sql` | `ai_call_log.course_id` |
| V1.1.1 学情聚合表 | `V1_1_1__course_statistics_job.sql` | `course_statistics` 日聚合结构 |
| V2.0 多租户核心 | `V2_0_0__multi_tenant_core.sql` | 租户/校区/组织/学期/记忆/OCR/导出/国密/干预 |
| V1.2.0 AI 模型运维 | `V1_2_0__ai_model_config_ops.sql` | `ai_model_config` 运维字段与 Embedding 默认模型 |
| V1.2.1 思考链字段 | `V1_2_1__ai_message_reasoning.sql` | `ai_message.reasoning_content` |
| V2.0.1 系统配置扩充 | `V2_0_1__system_config_expansion.sql` | `sys_sms_log`/`sys_email_log` + 12 组 `sys_config` |
| V2.0.2 核心表租户列 | `V2_0_2__legacy_core_tenant_id.sql` | `course`/`knowledge_base`/`ai_*`/`edu_question` 增加 `tenant_id` |
| V2.0.3 通知跳转 | `V2_0_3__notification_ref_id.sql` | `sys_notification.ref_id` + `idx_user_read` |
| V2.0.4 消息广播 | `V2_0_4__notification_broadcast.sql` | `priority` + `sys_notification_broadcast` |
| V2.0.5 广播权限 | `V2_0_5__notification_broadcast_permissions.sql` | `notice:broadcast:view/send` |
| V2.0.6 系统/智算权限 | `V2_0_6__system_and_ai_compute_permissions.sql` | 菜单/租户/配置/模型/网关等权限 |
| V2.0.7 记忆生命周期 | `V2_0_7__memory_retention.sql` | `retention_days`/`memory_type` + `ai:memory:*` 权限 |
| V2.0.8 模型Token上限 | `V2_0_8__ai_model_max_tokens_8192.sql` | Chat 模型默认 max_tokens 提升至 8192 |
| V2.0.9 课程 RAG Prompt | `V2_0_9__course_rag_prompt_templates.sql` | `prompt_template` 扩展字段 + 5 套教学提示词模板 |
| V2.0.10 通知多租户 | `V2_0_10__notification_tenant.sql` | 通知与广播表增加 `tenant_id` 隔离 + 权限赋权 (Gate I5) |
| V2.0.11 审计会话追溯 | `V2_0_11__ai_call_log_conversation_id.sql` | `ai_call_log.conversation_id`（无外键，可选追溯） |
| V2.0.12 OCR任务权限 | `V2_0_12__knowledge_ocr_permissions.sql` | 知识库 OCR 识别任务与校对使用权限 (Gate I6) |
| V2.0.13 轻量版提示词与Token | `V2_0_13__chat_rag_prompt_and_tokens.sql` | chat_rag 独立 System Prompt 与 max_tokens 提升至 8000 |
| V2.0.14 智能命题RAG模板 | `V2_0_14__exam_rag_prompt_template.sql` | 注入 EXAM_RAG_GENERAL 通用智能命题与溯源约束模板 |
| V2.0.15 智能批改RAG模板 | `V2_0_15__grading_rag_prompt_template.sql` | 注入 GRADING_RAG_GENERAL 评分点模型与证据溯源模板 |

示例（从 V0.2 升级到 V0.5）：

```bash
mysql -u root -p edumind < sql/migration/V0_5_0__product_enhancement.sql
mysql -u root -p edumind < sql/migration/V0_5_1__user_preferences.sql
```

示例（从 V1.0 升级到 V1.1）：

```bash
mysql -u root -p edumind < sql/migration/V1_0_1__agent_tool_permission.sql
mysql -u root -p edumind < sql/migration/V1_1_0__ai_call_log_course_id.sql
mysql -u root -p edumind < sql/migration/V1_1_1__course_statistics_job.sql
```

示例（从 V1.1 升级到 V2.0.12）：

```bash
mysql -u root -p edumind < sql/migration/V2_0_0__multi_tenant_core.sql
mysql -u root -p edumind < sql/migration/V1_2_0__ai_model_config_ops.sql
mysql -u root -p edumind < sql/migration/V1_2_1__ai_message_reasoning.sql
mysql -u root -p edumind < sql/migration/V2_0_1__system_config_expansion.sql
mysql -u root -p edumind < sql/migration/V2_0_2__legacy_core_tenant_id.sql
mysql -u root -p edumind < sql/migration/V2_0_3__notification_ref_id.sql
mysql -u root -p edumind < sql/migration/V2_0_4__notification_broadcast.sql
mysql -u root -p edumind < sql/migration/V2_0_5__notification_broadcast_permissions.sql
mysql -u root -p edumind < sql/migration/V2_0_6__system_and_ai_compute_permissions.sql
mysql -u root -p edumind < sql/migration/V2_0_7__memory_retention.sql
mysql -u root -p edumind < sql/migration/V2_0_8__ai_model_max_tokens_8192.sql
mysql -u root -p edumind < sql/migration/V2_0_9__course_rag_prompt_templates.sql
mysql -u root -p edumind < sql/migration/V2_0_10__notification_tenant.sql
mysql -u root -p edumind < sql/migration/V2_0_11__ai_call_log_conversation_id.sql
mysql -u root -p edumind < sql/migration/V2_0_12__knowledge_ocr_permissions.sql
```

> 全新建库请直接执行最新版 `sql/init.sql`（已含 V0.1~V2.0.12 与 V1.2.x 完整结构及种子），无需再跑 migration。

### V1.1 回滚（慎用，先备份）

```bash
mysqldump -u root -p edumind > backup_pre_v11_rollback.sql
mysql -u root -p edumind < sql/rollback/V1_1_rollback.sql
```

删除 `course_statistics` 表并移除 `ai_call_log.course_id` 列。详见 `sql/rollback/V1_1_rollback.sql`。

---

## 与后端的关系

- 后端**不包含**任何数据库迁移框架或自动建表逻辑，启动时只连接已有库。
- 表结构变更只改 `sql/` 下脚本，由你手动执行后再启动后端。
- 脚本 `V0_x_x__` 前缀仅用于版本对照，与运行时无关。

## 本地依赖服务

后端 **prod 环境强制依赖 Redis**（Sa-Token 会话、验证码、防重放、限流、RBAC 缓存、AI 会话态）。

本地需自行启动：

1. **MySQL 8**：`localhost:3306`，库名 `edumind`，账号 `root` / `root`
2. **Redis**：`localhost:6379`，无密码
3. **MinIO**（可选，文件上传）：`localhost:9000`

初始化数据库（**仅全新空库**；已有表请用 migration）：

```bash
mysql -u root -proot < sql/init.sql
```

### Gate H 隔离测试库（`edumind_gateh`）

Gate V11 集成测试使用独立库，详见 `backend/edu-mind-boot/src/test/resources/GATEH_README.md`。

```powershell
mysql -uroot -proot -e "CREATE DATABASE IF NOT EXISTS edumind_gateh CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
(Get-Content sql\init.sql -Raw -Encoding UTF8) -replace 'edumind','edumind_gateh' | mysql -uroot -proot --default-character-set=utf8mb4
Get-Content sql\migration\R__gate_h_e2e_seed.sql -Raw -Encoding UTF8 | mysql -uroot -proot edumind_gateh --default-character-set=utf8mb4
```

---

## 预置系统默认账号

| 用户名 | 登录密码 | 角色 | 姓名 | 说明 |
|:---|:---|:---|:---|:---|
| **admin** | `admin123` | 系统管理员 | 系统管理员 | 具备系统设置、用户与角色管理等全量权限 |
| **teacher** | `admin123` | 教师 | 张老师 | 负责备课授课、出题组卷、发布作业与批改 |
| **teacher2** | `admin123` | 教师 | 李老师 | Gate F 隔离测试专用（独占课程 104） |
| **student** | `admin123` | 学生 | 李同学 | 选课学习、在线作业提交、查看 AI 批改诊断 |
| **student2** | `admin123` | 学生 | 王同学 | 选课学生，具备选课与练习权限 |

---

## 67 张核心业务表全景清单

| 业务领域 | 数据表名 | Java Entity 实体映射 | 职责说明 |
|:---|:---|:---|:---|
| **系统管理** | `sys_user` | `UserEntity` | 用户基础信息（密码采用 BCrypt 加密） |
| | `sys_role` | `RoleEntity` | 角色表（ADMIN, TEACHER, STUDENT, TENANT_ADMIN, ORG_ADMIN） |
| | `sys_user_role` | `UserRoleEntity` | 用户与角色多对多映射 |
| | `sys_permission` | `PermissionEntity` | 细粒度操作权限编码 |
| | `sys_role_permission`| `RolePermissionEntity` | 角色与权限映射 |
| | `sys_notification` | `NotificationEntity` | 站内消息与作业通知（含 ref_id/priority） |
| | `sys_notification_broadcast` | - | 系统消息广播任务（V2.0.4） |
| | `sys_sms_log` | - | 短信发送审计日志（V2.0.1） |
| | `sys_email_log` | - | 邮件发送审计日志（V2.0.1） |
| | `sys_config` | - | 系统全局参数（邮件 SMTP、平台信息等） |
| | `sys_ai_quota` | - | AI Token / 调用配额 |
| | `sys_user_preference` | - | 用户偏好（主题/默认模型/RAG 开关） |
| **多租户 (V2.0)** | `sys_tenant` | `SysTenantEntity` | 学校租户主表 |
| | `sys_campus` | - | 校区信息 |
| | `sys_organization` | - | 院系/专业/班级组织树 |
| | `sys_term` | - | 学年学期配置 |
| | `sys_tenant_member` | - | 用户与租户成员绑定 |
| | `sys_member_org` | - | 成员组织分配关系 |
| | `sys_tenant_quota` | - | 租户资源配额（Token/存储/QPS/席位） |
| **课程教学** | `course` | `CourseEntity` | 课程主信息 |
| | `course_chapter` | `ChapterEntity` | 课程大纲层级章节树 |
| | `course_knowledge_point` | `KnowledgePointEntity` | 知识图谱核心知识点 |
| | `course_member` | `CourseMemberEntity` | 选课成员与教师绑定 |
| **题库试卷** | `question_bank` | `QuestionBankEntity` | 课程题库管理 |
| | `edu_question` | `QuestionEntity` | 试题（支持单选/多选/判断/填空/简答） |
| | `question_option` | - | 试题选项细项 |
| | `question_bank_item` | `QuestionBankItemEntity` | 题库与题目关联 |
| | `teaching_exam` | `ExamEntity` | 试卷测验信息 |
| | `exam_question` | `ExamQuestionEntity` | 试卷题目与分值排序 |
| **作业批改** | `assignment` | `AssignmentEntity` | 教学作业发布与截止控制 |
| | `assignment_submission` | `SubmissionEntity` | 学生作业提交记录 |
| | `submission_answer` | `SubmissionAnswerEntity` | 学生题目具体作答 |
| | `grading_result` | `GradingResultEntity` | AI 智能打分、评语与教师最终确认 |
| **知识管理** | `knowledge_base` | `KnowledgeBaseEntity` | 课程关联知识库 |
| | `knowledge_document` | `KnowledgeDocumentEntity` | 上传文档（PDF/Word/Markdown） |
| | `knowledge_document_text` | `KnowledgeDocumentTextEntity` | 向量化切片正文 |
| | `knowledge_document_chunk` | - | 文档 Chunk 切片 |
| | `knowledge_index_task` | - | 向量索引任务 |
| | `knowledge_chunk_index` | - | Chunk 与向量映射 |
| | `knowledge_ocr_task` | - | 多模态 OCR 识别任务（V2.0） |
| | `knowledge_ocr_page` | - | OCR 逐页识别与校对（V2.0） |
| **教学资源** | `teaching_resource` | `ResourceEntity` | 课件、教案与媒体资源 |
| | `course_resource` | `CourseResourceEntity` | 课程与资源/文档关联 |
| **AI 智能赋能**| `ai_tool` | `AiToolEntity` | AI 工具广场元数据 |
| | `ai_conversation` | `ConversationEntity` | AI 多轮会话记录 |
| | `ai_message` | `MessageEntity` | 对话消息历史与 Token 统计 |
| | `ai_call_log` | `AiCallLogEntity` | 大模型真实调用耗时与用量审计 |
| | `prompt_template` | - | Prompt 模板 |
| | `prompt_template_version` | - | Prompt 模板版本历史 |
| | `ai_memory_namespace` | - | AI 长期记忆命名空间（V2.0） |
| | `ai_memory_item` | - | AI 长期记忆条目（V2.0） |
| | `ai_memory_feedback` | - | AI 长期记忆反馈与纠错（V2.0） |
| **统计分析** | `statistics_daily_snapshot` | `StatisticsEntity` | 每日学情与 AI 消耗快照（供 ECharts 大屏） |
| | `learning_record` | - | 学习行为明细（V1.0） |
| | `knowledge_mastery` | - | 知识点掌握度（V1.0） |
| | `wrong_question_record` | - | 错题记录与归因（V1.0） |
| | `course_statistics` | `CourseStatisticsEntity` | 课程日聚合（活跃学生/均分/掌握度/AI 调用/错题，V1.1） |
| **知识图谱** | `knowledge_point_relation` | - | 知识点关系边（V1.0） |
| **AI Gateway** | `ai_model_config` | - | 模型配置与降级策略（V1.0） |
| | `ai_gateway_route` | - | 场景路由（CHAT/RAG/AGENT/GRADING）（V1.0） |
| **Agent** | `agent_run` | - | Agent 执行实例（V1.0） |
| | `agent_step` | - | Agent 步骤时间线（V1.0） |
| | `agent_tool_call` | - | Tool 调用日志（V1.0） |
| **导出与安全 (V2.0)** | `export_task` | - | 异步打印导出任务 |
| | `security_key_version` | - | 国密密钥版本元数据 |
| **教学决策 (V2.0)** | `teaching_intervention` | - | 教学干预建议决策 |
