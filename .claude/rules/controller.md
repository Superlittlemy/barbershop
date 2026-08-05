---
paths:
  - "src/main/java/**/controller/*Controller.java"
name: API开发规范
description: 项目API层的编码与安全标准
---

# Controller 层接口规范

## 规则

### 注解使用

- **@Schema**: 用于请求/响应 Model 类及其字段，提供 `description`
- **@Parameter**: 用于方法上标注路径参数，指定正确的 `in` 类型
- **@Operation**: 标注接口方法，`summary` 提供接口简短描述
- **@RequestBody**: 不需要额外 `@Parameter` 注解

### 返回包装

- 所有 Controller 接口返回必须使用 `ApiResponse<T>` 统一包装

### 列表分页

- 所有列表接口（返回集合的接口）必须支持分页，使用 `XxxQuery extends Page<T>` 接收分页参数及查询条件
- 默认排序字段由 `PageHandlerMethodArgumentResolver.DEFAULT_SORT_FIELD`（`created_time`）兜底，sort 参数格式：`field[,asc|desc]`，多个用 `,` 分隔
- Service 层需实现 `page(XxxQuery)` 接口，并返回 `PageResult<T>`，接口返回统一使用 `ApiResponse<PageResult<T>>`