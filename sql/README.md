# 智教云 · EduMind 数据库脚本中心

本项目所有数据库脚本统一维护在根目录 `sql/` 下。**后端启动不会自动改库**，需由你在 MySQL 中手动执行。

---

## 目录结构

```text
sql/
├── init.sql                                    # 全量单文件初始化（建库 + 33 张表 + 种子数据，含 V0.1~V0.5）
├── migration/                                  # 增量版本脚本（每个大版本一个文件）
│   ├── V0_1_0__mvp_core.sql                    # V0.1 MVP 核心（用户/课程/AI/题目/试卷/会话）
│   ├── V0_2_0__mvp_expansion.sql               # V0.2 MVP 扩展（题库/作业/文档/RBAC/通知/工具广场）
│   ├── V0_5_0__product_enhancement.sql         # V0.5 产品增强（Chunk/向量/RAG/Prompt/配额/权限）
│   └── R__seed_data.sql                        # 种子数据（可重复执行，注意幂等）
└── README.md
```

---

## 使用方式

### 方式一：一键全量初始化（新建库推荐）

适用于本地/测试库从零搭建：

```bash
mysql -u root -p < sql/init.sql
```

或在 Navicat / DataGrip 中打开并执行 `sql/init.sql`。

`init.sql` 已包含所有版本迁移脚本的最终表结构与种子数据，**无需再跑 migration**。

### 方式二：按版本增量迁移（已有空库分步升级）

在已创建 `edumind` 数据库的前提下，**每个大版本只执行一个脚本**：

```text
1. V0_1_0__mvp_core.sql
2. V0_2_0__mvp_expansion.sql
3. V0_5_0__product_enhancement.sql
4. R__seed_data.sql          # 可选，补充演示种子数据
```

示例：

```bash
mysql -u root -p edumind < sql/migration/V0_1_0__mvp_core.sql
mysql -u root -p edumind < sql/migration/V0_2_0__mvp_expansion.sql
mysql -u root -p edumind < sql/migration/V0_5_0__product_enhancement.sql
mysql -u root -p edumind < sql/migration/R__seed_data.sql
```

> **注意：** 若已执行过 `init.sql`，通常无需再跑 migration 脚本，避免重复建表。两种方式二选一即可。

### 已有库升级（按目标版本单脚本执行）

若库是早期版本建的，只需执行**尚未执行过的版本**对应脚本：

| 目标版本 | 迁移脚本 | 说明 |
|:---|:---|:---|
| V0.1 MVP 核心 | `V0_1_0__mvp_core.sql` | 用户、课程、AI、题目、试卷、会话 |
| V0.2 MVP 扩展 | `V0_2_0__mvp_expansion.sql` | 题库、作业批改、知识库文档、RBAC、通知、AI 工具广场 |
| V0.5 产品增强 | `V0_5_0__product_enhancement.sql` | Chunk、向量索引、RAG、Prompt 治理、配额、权限 |

示例（从 V0.2 升级到 V0.5）：

```bash
mysql -u root -p edumind < sql/migration/V0_5_0__product_enhancement.sql
```

> 全新建库请直接执行最新版 `sql/init.sql`（已含完整结构及种子），无需再跑 migration。

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

初始化数据库：

```bash
mysql -u root -proot < sql/init.sql
```

---

## 预置系统默认账号

| 用户名 | 登录密码 | 角色 | 姓名 | 说明 |
|:---|:---|:---|:---|:---|
| **admin** | `admin123` | 系统管理员 | 系统管理员 | 具备系统设置、用户与角色管理等全量权限 |
| **teacher** | `admin123` | 教师 | 张老师 | 负责备课授课、出题组卷、发布作业与批改 |
| **student** | `admin123` | 学生 | 李同学 | 选课学习、在线作业提交、查看 AI 批改诊断 |
| **student2** | `admin123` | 学生 | 王同学 | 选课学生，具备选课与练习权限 |

---

## 33 张核心业务表全景清单

| 业务领域 | 数据表名 | Java Entity 实体映射 | 职责说明 |
|:---|:---|:---|:---|
| **系统管理** | `sys_user` | `UserEntity` | 用户基础信息（密码采用 BCrypt 加密） |
| | `sys_role` | `RoleEntity` | 角色表（ADMIN, TEACHER, STUDENT） |
| | `sys_user_role` | `UserRoleEntity` | 用户与角色多对多映射 |
| | `sys_permission` | `PermissionEntity` | 细粒度操作权限编码 |
| | `sys_role_permission`| `RolePermissionEntity` | 角色与权限映射 |
| | `sys_notification` | `NotificationEntity` | 站内消息与作业通知 |
| | `sys_config` | - | 系统全局参数（邮件 SMTP、平台信息等） |
| | `sys_ai_quota` | - | AI Token / 调用配额 |
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
| **教学资源** | `teaching_resource` | `ResourceEntity` | 课件、教案与媒体资源 |
| | `course_resource` | `CourseResourceEntity` | 课程与资源/文档关联 |
| **AI 智能赋能**| `ai_tool` | `AiToolEntity` | AI 工具广场元数据 |
| | `ai_conversation` | `ConversationEntity` | AI 多轮会话记录 |
| | `ai_message` | `MessageEntity` | 对话消息历史与 Token 统计 |
| | `ai_call_log` | `AiCallLogEntity` | 大模型真实调用耗时与用量审计 |
| | `prompt_template` | - | Prompt 模板 |
| | `prompt_template_version` | - | Prompt 模板版本历史 |
| **统计分析** | `statistics_daily_snapshot` | `StatisticsEntity` | 每日学情与 AI 消耗快照（供 ECharts 大屏） |
