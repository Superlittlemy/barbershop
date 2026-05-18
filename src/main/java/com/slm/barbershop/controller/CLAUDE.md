# Controller 层接口规范

## API 文档注释规则

### 1. 请求/响应模型（Model）

使用 `@Schema` 注解描述字段，包含 description 和 example：

```java
@Data
@Schema(description = "店铺请求")
public class ShopRequest {

    @Schema(description = "店铺名称")
    private String name;

    @Schema(description = "联系电话")
    private String phone;

}
```

### 2. 路径参数（@PathVariable）

使用 `@Parameter` 注解在方法上标注：

```java
@Operation(summary = "获取店铺")
@Parameter(name = "id", description = "店铺ID", in = ParameterIn.PATH)
@GetMapping("/{id}")
public ShopResponse getById(@PathVariable Long id) {
    // ...
}
```

### 3. @RequestBody 参数

`@RequestBody` 参数不需要加 `@Parameter`，Swagger 会根据 Model 类的 `@Schema` 注解自动生成文档。

## 规则说明

- **@Schema**: 用于请求/响应模型类及其字段，提供字段的中文描述和示例值
- **@Parameter**: 用于方法上，描述路径参数，必须指定 `in = ParameterIn.PATH`
- **@Operation**: 标注接口方法，summary 提供接口简短描述
- **@RequestBody**: 不需要额外 `@Parameter` 注解

## 示例

| 场景 | 注解位置 | 示例 |
|------|----------|------|
| 请求/响应 Model 类 | 类上 | `@Schema(description = "xxx")` |
| Model 字段 | 字段上 | `@Schema(description = "xxx")` |
| @PathVariable | 方法上 | `@Parameter(name = "id", description = "店铺ID", in = ParameterIn.PATH)` |
| @RequestBody | 不标注 | 直接使用，文档由 Model 类的 @Schema 生成 |