# Gate H 集成测试环境

Gate V11 集成测试使用**隔离数据库** `edumind_gateh`，禁止在共享 `edumind` 开发库上运行 DDL 或破坏性初始化。

## 前置条件

- MySQL 8 @ `localhost:3306`（账号 `root` / `root`）
- Redis @ `localhost:6379`
- JDK 17

## 初始化隔离库（首次或重置）

```powershell
# 1. 创建库
mysql -uroot -proot -e "CREATE DATABASE IF NOT EXISTS edumind_gateh CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# 2. 全量结构 + 种子（将 init.sql 中的库名替换为 edumind_gateh）
cd E:\EduMind
(Get-Content sql\init.sql -Raw -Encoding UTF8) -replace 'edumind','edumind_gateh' | mysql -uroot -proot --default-character-set=utf8mb4

# 3. Gate F/G/H E2E 种子（幂等）
Get-Content sql\migration\R__gate_e2e_seeds.sql -Raw -Encoding UTF8 | mysql -uroot -proot edumind_gateh --default-character-set=utf8mb4
```

## 运行测试

```bash
cd backend
mvn clean test -pl edu-mind-boot -am "-DgateH.integration=true" "-Dtest=GateV11IntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false"
```

Profile：`test` + `gateh`（见 `application-gateh.yml`）。

## 回滚与备份演练

```powershell
# 备份
mysqldump -uroot -proot edumind_gateh > backup_gateh_YYYYMMDD.sql

# V1.1 回滚（生产库 edumind 慎用，先备份）
mysql -uroot -proot edumind < sql/rollback/V1_1_rollback.sql

# 从备份恢复到临时库验证
mysql -uroot -proot -e "CREATE DATABASE IF NOT EXISTS edumind_restore;"
mysql -uroot -proot edumind_restore < backup_gateh_YYYYMMDD.sql
```

回滚脚本：`sql/rollback/V1_1_rollback.sql`（删除 `course_statistics`、移除 `ai_call_log.course_id`）。

## 注意事项

- `GateV11IntegrationTest` **不再**在 `@BeforeEach` 中执行 `DROP TABLE` 等 DDL。
- 数据依赖 `init.sql`（或等价 migration）+ `R__gate_e2e_seeds.sql`。
- 若测试报 `Unknown database 'edumind_gateh'`，请按上文初始化。
