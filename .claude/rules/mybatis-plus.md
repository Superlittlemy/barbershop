---
paths:
  - "src/main/java/**/service/*.java"
  - "src/main/java/**/mapper/*.java"
  - "src/main/java/config/EntityMetadataConfig.java"
name: MyBatis-Plus 编码规范
description: MyBatis-Plus 在 Service/Mapper 层的常见使用模式与陷阱,覆盖更新策略、自动填充、逻辑删除、查询条件
---

# MyBatis-Plus 编码规范

## 1. UpdateStrategy.NOT_NULL 默认行为

`updateById(entity)` 与 `update(entity, wrapper)` 默认遵循 **`UpdateStrategy.NOT_NULL`**:entity 中为 null 的字段**不会**出现在 SET 子句里。

- 想把字段清成 NULL(例如解除唯一键占位、清快照字段),**不能**只 `entity.setXxx(null)` 然后 `updateById(entity)`——SET 子句不会包含该字段,值保持不变。
- 必须用 `UpdateWrapper.set(field, null)` 或 `LambdaUpdateWrapper.set(S::getXxx, null)`,wrapper 的 set 会绕过 NOT_NULL 过滤、显式写入 `SET xxx = NULL`。

## 2. entity 为 null 时不会触发 MetaObjectHandler

`MetaObjectHandler.updateFill(metaObject)` 需要非空的 metaObject 才能识别需要填充的字段并写入。

- ❌ `mapper.update(null, wrapper)` —— entity 为 null,**自动填充完全失效**:`updatedBy` / `updatedTime` 等 `@TableField(fill = FieldFill.UPDATE)` 字段不会被写入。
- ✅ `mapper.update(entity, wrapper)` —— entity 非空时 MetaObjectHandler 在生成 SQL 之前回调,写入的字段值随 entity 一起进入 SET。
- ✅ `mapper.updateById(entity)` —— 同样依赖非空 entity。

## 3. wrapper.set 与 entity 字段的合并顺序

`update(entity, wrapper)` 生成 SET 子句时:

1. 先由 `MetaObjectHandler.updateFill` 把 `updatedBy` / `updatedTime` 写入 entity
2. 再扫描 entity 上的非 null 字段
3. 最后合并 wrapper.set(...) 的字段,**wrapper.set 优先于 entity 同名字段**

含义:可以**复用 selectById 拿到的 entity** 作为 update entity,无需构造 stub;同一字段若 wrapper.set 显式指定了值,会覆盖 entity 上的旧值。

## 4. 触发自动填充的两种规范写法

```java
// 写法 A:wrapper 携带部分字段,entity 复用 selectById 结果或新构造并 setId
// MetaObjectHandler 自动写入 updatedBy/updatedTime;wrapper.set 显式覆盖目标字段
Employee employee = employeeMapper.selectById(id);
employeeMapper.update(employee,
    new LambdaUpdateWrapper<Employee>()
        .eq(Employee::getId, id)
        .set(Employee::getEmployeeNo, null));

// 写法 B:updateById,纯 entity 字段更新(无法清字段为 NULL)
employee.setName(newName);
employeeMapper.updateById(employee);
```

## 5. 反例

- ❌ `mapper.update(null, wrapper)` 后再 `mapper.updateById(entity)`,认为两步能合并——前一步丢失 `updatedBy`/`updatedTime`,审计/乐观锁会异常
- ❌ 想"清字段为 NULL"却使用 `entity.setX(null) + updateById(entity)`——SET 子句根本不包含该字段
- ❌ 在 service 里手工 `employee.setUpdatedBy(userId)` / `employee.setUpdatedTime(LocalDateTime.now())`——绕过 `EntityMetadataConfig`,丧失统一审计入口
- ❌ 重新构造 `Employee()` stub 然后只 `setId(...)` 复用 entity 通道——在没有 selectById 上下文时 OK,但若有现成实体,直接复用即可,见 §3

## 6. 与逻辑删除字段的协同

`@TableLogic(value = "0", delval = "1")` 会在 UPDATE/DELETE 上追加 `WHERE is_deleted = 0`。软删后:

- `updateById(entity)` 0 行变更(行已被逻辑删除过滤)
- `mapper.update(entity, wrapper.eq(S::getId, id))` 同样 0 行变更
- **软删后再做附加更新**(例如清号),必须先清号再软删;或绕开逻辑删除过滤(原生 SQL / 关闭 `@TableLogic` 的拦截器)
- `removeById(id)` / `mapper.deleteById(id)` 不受 NOT_NULL 策略影响,可正常置 `is_deleted = 1`

## 7. 与 EntityMetadataConfig 的契约

`EntityMetadataConfig` 是项目内统一的字段填充入口。所有"自动维护"字段(`createdBy` / `createdTime` / `updatedBy` / `updatedTime` / `isDeleted` / `version`)的赋值**只走这里**,Service/Controller 不直接 set 这些字段。需要当前用户信息用 `UserContext.getUser()`,MetaObjectConfig 内部已 null-safe 调用。

## 8. 查询条件与软删过滤

- 默认所有查询(单表/分页)自动追加 `WHERE is_deleted = 0`,无需在 wrapper 里再写。
- 想绕过逻辑删除查询历史/审计数据,用 `.last("")` 拼 SQL 或自定义 mapper 方法,避免漏写 `is_deleted` 条件把脏数据带出。
- `selectById` 受 `@TableLogic` 影响:软删行查不到(返回 null),删除前需 `selectById` 校验存在性。

## 9. 乐观锁

带 `@Version` 的字段在 `updateById` / `update` 时,MyBatis-Plus 会自动追加 `WHERE version = ?`,并在 SET 子句里 `version = version + 1`。**禁止手动 set version**,绕过会导致乐观锁失效并发写覆盖。

## 10. 分页与排序

- 列表查询统一接收 `XxxQuery extends Page<T>`,由 `PageHandlerMethodArgumentResolver` 解析 `sort` 参数(`field[,asc|desc]`,多个用 `,` 分隔,默认 `created_time`)。
- wrapper 里**不重复声明默认排序**(`orderByDesc` / `orderByAsc`),交由框架层控制;只有"非默认排序"才在 wrapper 里 `orderByXxx`。