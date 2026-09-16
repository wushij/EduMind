# 智教云 · EduMind 数据库脚本中心

本项目所有数据库脚本统一维护在根目录 `sql/` 下。**后端启动不会自动改库**，需由你在 MySQL 中手动执行。

---

## 目录结构

```text
sql/
├── init.sql                                    # 安全初始化（仅 IF NOT EXISTS 建表 + INSERT IGNORE 种子，**不 DROP**）
├── migration/                                  # 增量版本脚本（V0 细粒度 + V1/V2 合并大文件）
│   ├── V0_1_0__mvp_core.sql                    # V0.1 MVP 核心（用户/课程/AI/题目/试卷/会话）
│   ├── V0_2_0__mvp_expansion.sql               # V0.2 MVP 扩展（题库/作业/文档/RBAC/通知/工具广场）
│   ├── V0_5_0__product_enhancement.sql         # V0.5 产品增强（Chunk/向量/RAG/Prompt/配额/权限）
│   ├── V0_5_1__user_preferences.sql            # V0.5.1 用户偏好设置
│   ├── V1_0__intelligent_hub_and_analytics.sql # V1.0~V1.1 智能中枢/学情/Agent/Gateway（合并）
│   ├── V1_1__ai_model_enhancements.sql         # V1.2 AI 模型运维与思考链（合并，须在 V2_0_0 后执行）
│   ├── V2_0_0__multi_tenant_and_notification.sql # V2.0.0~V2.0.6 多租户/通知/权限（合并）
│   ├── V2_0_1__ai_memory_and_rag_foundation.sql  # V2.0.7~V2.0.13 记忆/RAG/OCR（合并）
│   ├── V2_0_2__rag_templates_gates_audit.sql   # V2.0.14~V2.0.19 RAG模板/Gate/操作日志（合并）
│   ├── V2_0_3__security_key_kms.sql            # V2.0.20 国密 KMS 租户密钥版本管理（Gate I9）
│   ├── V2_0_4__ai_model_key_version.sql        # V2.0.21 AI 模型 API Key 接入 KMS（Gate I10）
│   ├── V2_0_5__sys_org_quota.sql               # V2.0.22 组织院系算力配额表（sys_org_quota）
│   ├── V2_0_6__oper_log_tenant_hardening.sql   # V2.0.23 操作日志多租户加固与审计闭环（Gate I11）
│   ├── V2_1_0__ai_tool_admin.sql               # V2.1.0 AI 工具管理端字段与 system:tool:edit 权限
│   ├── V2_2_0__tenant_wave1.sql                # GA Wave1：19 张高泄漏风险子表 tenant_id
│   ├── V2_2_1__tenant_wave2.sql                # GA Wave2：16 张剩余业务表 tenant_id
│   ├── V2_2_2__expand_permission_catalog.sql   # V2.2.2 权限目录补全（与前端 DEFAULT 对齐）
│   ├── V2_2_3__course_ai_and_attributes_expansion.sql # V2.2.3 课程学分/学时/AI 人设等字段
│   ├── V2_2_4__course_overview_portal.sql      # V2.2.4 课程概览：教学目标/公告/教学团队
│   ├── V2_2_5__sys_menu.sql                    # V2.2.5 系统菜单表 + assignment:delete 权限
│   ├── R__seed_data.sql                        # V0 增量路径演示种子（用户/课程/题库/AI 等，幂等）
│   ├── R__seed_legacy.sql                      # 旧库升级补丁（租户角色/组织成员/权限乱码修复，幂等）
│   └── R__gate_e2e_seeds.sql                   # Gate F/G/H 集成测试种子（幂等，init 不含 Gate F）
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

`init.sql` 已包含 **V0.1 ~ V2.2.5** 迁移最终状态（含多租户 Wave1/2、权限补全、课程 AI 字段、课程概览门户表、`sys_menu`、RAG/国密 KMS/组织配额/操作日志等），**全新空库跑 init 后无需再跑 `V*.sql` migration**（Gate E2E 可选种子除外）。

### 方式二：按版本增量迁移（已有空库分步升级）

在已创建 `edumind` 数据库的前提下，**按顺序执行以下脚本**（V1/V2 已合并为少量大文件）：

```text
1.  V0_1_0__mvp_core.sql
2.  V0_2_0__mvp_expansion.sql
3.  V0_5_0__product_enhancement.sql
4.  V0_5_1__user_preferences.sql
5.  V1_0__intelligent_hub_and_analytics.sql   # V1.0 + V1.1
6.  V2_0_0__multi_tenant_and_notification.sql # V2.0.0 ~ V2.0.6
7.  V1_1__ai_model_enhancements.sql          # V1.2（须在步骤 6 之后）
8.  V2_0_1__ai_memory_and_rag_foundation.sql # V2.0.7 ~ V2.0.13
9.  V2_0_2__rag_templates_gates_audit.sql     # V2.0.14 ~ V2.0.19
10. V2_0_3__security_key_kms.sql              # V2.0.20 国密 KMS (Gate I9)
11. V2_0_4__ai_model_key_version.sql          # V2.0.21 AI 模型密钥 KMS (Gate I10)
12. V2_0_5__sys_org_quota.sql                 # V2.0.22 组织院系算力配额
13. V2_0_6__oper_log_tenant_hardening.sql     # V2.0.23 操作日志多租户加固 (Gate I11)
14. V2_1_0__ai_tool_admin.sql                 # V2.1.0 AI 工具 sort_order/update_time + 编辑权限
15. V2_2_0__tenant_wave1.sql                 # V2.2.0 GA Wave1 tenant_id
16. V2_2_1__tenant_wave2.sql                 # V2.2.1 GA Wave2 tenant_id
17. V2_2_2__expand_permission_catalog.sql    # V2.2.2 权限补全
18. V2_2_3__course_ai_and_attributes_expansion.sql
19. V2_2_4__course_overview_portal.sql
20. V2_2_5__sys_menu.sql
21. R__seed_data.sql          # 可选，V0 增量路径补充演示数据
22. R__seed_legacy.sql        # 可选，旧库升级补丁（租户 RBAC / 组织成员）
23. R__gate_e2e_seeds.sql     # 可选，Gate F/G/H 集成测试专用
```

> **执行顺序说明：** V1.2 依赖 V2.0 多租户表结构，因此 `V1_1__*` 插在 `V2_0_0__*` 与 `V2_0_1__*` 之间，与历史细粒度脚本顺序一致。

示例：

```bash
mysql -u root -p edumind < sql/migration/V0_1_0__mvp_core.sql
mysql -u root -p edumind < sql/migration/V0_2_0__mvp_expansion.sql
mysql -u root -p edumind < sql/migration/V0_5_0__product_enhancement.sql
mysql -u root -p edumind < sql/migration/V0_5_1__user_preferences.sql
mysql -u root -p edumind < sql/migration/V1_0__intelligent_hub_and_analytics.sql
mysql -u root -p edumind < sql/migration/V2_0_0__multi_tenant_and_notification.sql
mysql -u root -p edumind < sql/migration/V1_1__ai_model_enhancements.sql
mysql -u root -p edumind < sql/migration/V2_0_1__ai_memory_and_rag_foundation.sql
mysql -u root -p edumind < sql/migration/V2_0_2__rag_templates_gates_audit.sql
mysql -u root -p edumind < sql/migration/V2_0_3__security_key_kms.sql
mysql -u root -p edumind < sql/migration/V2_0_4__ai_model_key_version.sql
mysql -u root -p edumind < sql/migration/V2_0_5__sys_org_quota.sql
mysql -u root -p edumind < sql/migration/V2_0_6__oper_log_tenant_hardening.sql
mysql -u root -p edumind < sql/migration/R__seed_data.sql
mysql -u root -p edumind < sql/migration/R__seed_legacy.sql
```

> **注意：** 若已执行过 `init.sql`，通常无需再跑 migration 脚本，避免重复建表。两种方式二选一即可。  
> 若库是 V2.0.2 之前建的旧库，需额外执行 `R__seed_legacy.sql` 补齐租户 RBAC 与组织成员。  
> 若旧库已执行过细粒度 `V1_*` / `V2_0_*` 脚本，**勿重复执行**对应合并文件；仅补跑尚未执行过的合并脚本即可（脚本内多为 `IF NOT EXISTS` / `INSERT IGNORE`，但重复 ALTER 仍可能报错）。

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
| V1.0~V1.1 智能中枢与学情 | `V1_0__intelligent_hub_and_analytics.sql` | 学情/图谱/Gateway/Agent + `ai:tool:use` + `course_statistics` + `ai_call_log.course_id` |
| V2.0.0~V2.0.6 多租户与通知 | `V2_0_0__multi_tenant_and_notification.sql` | 租户/组织/记忆/OCR/导出/国密/干预 + 系统配置/通知广播/权限种子 |
| V1.2 AI 模型增强 | `V1_1__ai_model_enhancements.sql` | `ai_model_config` 运维字段 + `ai_message.reasoning_content`（须在 V2_0_0 后） |
| V2.0.7~V2.0.13 记忆与 RAG 基础 | `V2_0_1__ai_memory_and_rag_foundation.sql` | 记忆生命周期/课程 RAG Prompt/OCR 权限/通知多租户/conversation_id |
| V2.0.14~V2.0.19 Gate 与审计 | `V2_0_2__rag_templates_gates_audit.sql` | EXAM/GRADING/LESSON_PREP RAG 模板 + 导出异步 + 干预权限 + `sys_oper_log` |
| V2.0.20 国密 KMS | `V2_0_3__security_key_kms.sql` | 租户数据密钥版本管理 `security_key_version` + 记忆版本追踪 (Gate I9) |
| V2.0.21 AI 模型密钥 KMS | `V2_0_4__ai_model_key_version.sql` | `ai_model_config.key_version` + 平台模型密钥种子 `edumind-model-key` (Gate I10) |
| V2.0.22 组织院系配额 | `V2_0_5__sys_org_quota.sql` | `sys_org_quota` 表 + 组织级 TOKEN/STORAGE/SEATS 演示种子 |
| V2.0.23 操作日志加固 | `V2_0_6__oper_log_tenant_hardening.sql` | `sys_oper_log` 多租户索引与 operlog 权限闭环 (Gate I11) |

示例（从 V0.2 升级到 V0.5）：

```bash
mysql -u root -p edumind < sql/migration/V0_5_0__product_enhancement.sql
mysql -u root -p edumind < sql/migration/V0_5_1__user_preferences.sql
```

示例（从 V0.5 一次性升级到最新 V2.0.23）：

```bash
mysql -u root -p edumind < sql/migration/V1_0__intelligent_hub_and_analytics.sql
mysql -u root -p edumind < sql/migration/V2_0_0__multi_tenant_and_notification.sql
mysql -u root -p edumind < sql/migration/V1_1__ai_model_enhancements.sql
mysql -u root -p edumind < sql/migration/V2_0_1__ai_memory_and_rag_foundation.sql
mysql -u root -p edumind < sql/migration/V2_0_2__rag_templates_gates_audit.sql
mysql -u root -p edumind < sql/migration/V2_0_3__security_key_kms.sql
mysql -u root -p edumind < sql/migration/V2_0_4__ai_model_key_version.sql
mysql -u root -p edumind < sql/migration/V2_0_5__sys_org_quota.sql
mysql -u root -p edumind < sql/migration/V2_0_6__oper_log_tenant_hardening.sql
```

> 全新建库请直接执行最新版 `sql/init.sql`（已含 V0.1~V2.0.23 与 V1.2.x 完整结构及种子），无需再跑 migration。

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
Get-Content sql\migration\R__gate_e2e_seeds.sql -Raw -Encoding UTF8 | mysql -uroot -proot edumind_gateh --default-character-set=utf8mb4
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

## 68 张核心业务表全景清单

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
| | `sys_org_quota` | `SysOrgQuotaEntity` | 组织院系算力配额分配表（Gate I11/V2.0.5） |
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
| **AI Gateway** | `ai_model_config` | `AiModelConfigEntity` | 模型配置与降级策略 (Gate I10 SM4-GCM) |
| | `ai_gateway_route` | - | 场景路由（CHAT/RAG/AGENT/GRADING）（V1.0） |
| **Agent** | `agent_run` | - | Agent 执行实例（V1.0） |
| | `agent_step` | - | Agent 步骤时间线（V1.0） |
| | `agent_tool_call` | - | Tool 调用日志（V1.0） |
| **导出与安全 (V2.0)** | `export_task` | `ExportTaskEntity` | 异步打印导出任务 |
| | `security_key_version` | `SecurityKeyVersionEntity` | 国密密钥版本元数据 (Gate I9) |
| **教学决策 (V2.0)** | `teaching_intervention` | `TeachingInterventionEntity` | 教学干预建议决策 (Gate I8) |
| **系统与审计 (V2.0)** | `sys_oper_log` | `SysOperLogEntity` | 业务操作日志审计闭环 (Gate I11) |
