---
paths:
  - "src/main/java/**/entity/*.java"
name: 业务实体规范
description: 项目实体层的编码标准
---

# 业务实体规范

## 规则

### 继承要求

- 所有业务实体必须继承 `BaseEntity`

### BaseEntity 通用字段

- `id`: 主键
- `createdBy`: 创建人
- `createdTime`: 创建时间
- `updatedBy`: 更新人
- `updatedTime`: 更新时间
- `isDeleted`: 逻辑删除标记

### 继承优势

- 统一的 ID 生成策略
- 自动填充创建/更新信息
- 逻辑删除支持
- 一致的实体管理