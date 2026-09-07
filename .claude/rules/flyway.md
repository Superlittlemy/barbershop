---
paths:
  - "src/main/resources/db/migration/V*.sql"
name: Flyway 版本脚本规范
description: src/main/resources/db/migration 是 Flyway 版本脚本目录,V 开头的脚本一旦提交不允许再次编辑
---

# Flyway 版本脚本规范

## 目录定位

`src/main/resources/db/migration/` 是 Flyway 版本脚本目录,文件名 `V<版本号>__<描述>.sql`,启动时按版本号顺序应用,并写入 `flyway_schema_history` 表。

## 强制规则

### V 开头的脚本一旦创建不允许编辑

- Flyway 通过**文件名 + 已校验的 checksum**识别版本脚本,已应用的脚本被修改会抛 `FlywayValidateException: Migration checksum mismatch`,导致应用启动失败。
- 任何对历史版本脚本的"小修小补"必须**新建一个 V 脚本**(如 `V12__xxx.sql`)来承载,而不是改 V1~V11。
- 在脚本未应用(本地未提交/未上线)阶段,若需修正语法或拆分语句,允许编辑;一旦进入 flyway_schema_history,即视为不可变。

### 新建脚本的要求

- 版本号取当前最大值 +1,文件名遵守 `V<版本号>__<简短描述>.sql` 格式(双下划线,小写英文/数字)。
- 单次迁移职责单一:不要把多个无关变更塞进同一个脚本。
- DDL 与 DML 顺序:先 `CREATE/ALTER` 再 `INSERT/UPDATE`,便于回放。
- 涉及回填/批量更新时,脚本需在事务内执行且幂等(可用 `WHERE ... IS NULL` 守卫,避免重复应用报错)。

### 兼容性约束

- 严禁 `DROP COLUMN` 已上线数据中可能被引用的列(若确实需要,先 `ALTER ... DROP ...` 配合应用层灰度)。
- `ALTER TABLE` 大表操作需考虑锁表;DDL 改列类型时先评估 `NOT NULL`/`DEFAULT` 影响。
- 已有 `uk_*` 唯一键的扩展:必须新增迁移脚本调整索引,而不是假设数据库现状。

## 反例

- ❌ 编辑 `V10__add_employee.sql` 修改 `employee_no` 列定义
- ❌ 复用 `V11__bill_persist_names.sql` 添加与"账单姓名持久化"无关的 DDL
- ❌ 修改历史脚本的注释/PR 文案,以"只改注释不触发校验"为由绕过规则
- Flyway 默认对整个文件做 checksum,包括注释,**任何字节修改都触发 checksum mismatch**