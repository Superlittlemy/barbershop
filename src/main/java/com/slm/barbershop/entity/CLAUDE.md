# 业务实体规范

## 规则

**所有业务实体必须继承 `BaseEntity`**。

`BaseEntity` 提供了以下通用字段：
- `id`: 主键
- `createdBy`: 创建人
- `createdTime`: 创建时间
- `updatedBy`: 更新人
- `updatedTime`: 更新时间
- `isDeleted`: 逻辑删除标记

通过继承 `BaseEntity`，业务实体自动获得：
1. 统一的 ID 生成策略
2. 自动填充创建/更新信息
3. 逻辑删除支持
4. 一致的实体管理
