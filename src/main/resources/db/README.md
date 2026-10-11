# 数据库变更管理（db/）

> 全量表结构权威源：`design/多多记账-技术文档-数据库设计.docx` 与 `schema.sql`（最新全量 DDL）。
> 本目录管理"随代码流转的增量变更"，规则如下。

## 文件构成

| 文件 | 性质 | 说明 |
|---|---|---|
| `../schema.sql` | 全量 DDL（幂等） | 新库初始化唯一入口，含全部 20 表 + `CREATE DATABASE`；CI 每次重建 |
| `init_category_data.sql` | 种子数据 | 系统预置分类，初始化时随 schema.sql 之后执行 |
| `r002-incremental.sql` | 历史增量存档 | R002 新增 notification/import_batch（已并入 schema.sql，仅作变更历史） |
| `r003-incremental.sql` | 历史增量存档 | R003 新增 recalculation_task（同上） |
| `r004-incremental.sql` | 历史增量存档 | R004 新增 monthly_report（同上） |

## 规则（新增表结构变更时）

1. **新需求改表 → 新建增量文件**：`db/rXXX-incremental.sql`（XXX=需求编号），只写增量
   （`CREATE TABLE IF NOT EXISTS` / `ALTER TABLE` / 数据订正），并同步更新 `schema.sql`
   全量 DDL 保持一致（schema.sql 永远反映"从零初始化后的最新形态"）。
2. **开发期**：本地 `scripts/init_local_db.ps1` 重建本地库（schema.sql + 种子），或手工对
   本地库执行增量。
3. **测试期**（feature → test 合并时）：由执行人在 RDS duoduocode_dev 上重放增量文件
   （半自动：按编号顺序执行，执行前确认当前表结构状态）。
4. **正式期**（test → main 合并时）：同样在 RDS duoduocode_prod 重放，重放前先备份
   （`mysqldump` 快照）。
5. 增量文件**不可修改已发布内容**——发现写错就新增订正增量，不改历史文件。

## 幂等性要求

- 建表一律 `CREATE TABLE IF NOT EXISTS`；
- 加列前先判断（MySQL 8.0 无 `ADD COLUMN IF NOT EXISTS`，用存储过程判断或直接接受
  二次执行报错并跳过——重放人负责判断）；
- 种子 INSERT 前先 `DELETE` 对应范围或用唯一键 `ON DUPLICATE KEY`。
