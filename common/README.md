# common 模块

通用基础组件库，为所有业务模块提供"开箱即用"的能力：统一响应、异常处理、分页、字段自动填充、日志链路追踪、CORS、接口文档、Controller 日志切面等。

**Maven 模块坐标**：`com.slm:common`（由父 POM `barbershop` 统一管理版本）

---

## 1. 模块结构

```
common
├── pom.xml
└── src/main
    ├── java/com/slm/common
    │   ├── config/        # Spring 自动装配（@Configuration / @Component / @Aspect）
    │   ├── context/       # 请求级 ThreadLocal 上下文
    │   ├── entity/        # 实体基类
    │   ├── enums/         # 通用枚举
    │   ├── exception/     # 业务异常 + 统一异常处理
    │   ├── model/         # 响应/分页包装、认证用户
    │   └── utils/         # 工具方法
    └── resources/
        └── logback-spring.xml   # 默认日志配置（含 traceId 输出）
```

---

## 2. 能力清单

### 2.1 `config/` — Spring 自动装配

以下 Bean **绝大多数**在业务模块把 common 引入 classpath 并被 Spring 扫描到后会自动生效，**无需 `@Import`**。个别例外见表格内说明及 §3.5。

| 类 | 作用 | 关键行为 |
|---|---|---|
| `ApiLogAspect` | Controller 切面日志 | 切点 `com.slm..controller..*.*`；`MultipartFile` 脱敏只打印文件名和大小；响应体经 Jackson 序列化后打印 |
| `GlobalCorsConfig` | 全局 CORS | 当前为 `addAllowedOrigin("*")`、放行所有方法和 Header，**生产环境务必收紧** |
| `JsonConfig` | Jackson `ObjectMapper`（`@Primary`） | `LocalDateTime` 序列化为 `yyyy-MM-dd HH:mm:ss`；**全局 `Long` 序列化为字符串**，防止前端 JS 精度丢失；反序列化忽略未知字段 |
| `PageHandlerMethodArgumentResolver` | Controller 参数 `Page<T>` 自动绑定 | 支持 `current / size / sort`；`size` 上限 1000；`sort` 字段必须为实体字段（含主键）防 SQL 注入；排序支持驼峰或下划线，方向 `asc/desc`（缺省 desc） |
| `EntityMetadataConfig` | MyBatis-Plus `MetaObjectHandler` | insert 填 `createdBy / createdTime / isDeleted=0`；update 填 `updatedBy / updatedTime`。**依赖 `UserContext.getUser()`，未登录场景对应字段为 `null`** |
| `MyBatisPlusConfig` | MyBatis-Plus 拦截器 | 注册分页拦截器（`DbType.MYSQL`）和乐观锁拦截器 |
| `WildcardMapperScanRegistrar` | 通配 Mapper 扫描器 | 实现 `ImportBeanDefinitionRegistrar`；扫描 `com.slm.*.mapper` 子包下所有接口并通过 `MapperScannerConfigurer` 注册。**与表格中其它 Bean 不同——它不会随包扫描自动生效**，必须在每个业务 `@SpringBootApplication` 上显式 `@Import(WildcardMapperScanRegistrar.class)`（详见 §3.5）。约定：所有 Mapper 接口必须落在 `com.slm.<module>.mapper` 包下，新增模块无需在此处追加配置 |
| `SwaggerConfig` | SpringDoc OpenAPI | 绑定 `application.yaml` 中 `springdoc-info.*`（title / description / version / contact-name），固定 `Apache License 2.0` |
| `TraceFilter` + `TraceIdConverter` + `logback-spring.xml` | 请求级 traceId 链路追踪 | 见 §3.4 |
| `WebMVCConfig` | MVC 配置 | 注册分页参数解析器 |
| `GlobalExceptionHandler` | `@RestControllerAdvice` 统一异常 | 见 §3.5 |

### 2.2 `context/` — 线程级上下文

| 类 | 说明 |
|---|---|
| `UserContext` | `ThreadLocal<AuthUser>`。JWT 过滤器在请求开始时 `setUser`，结束 `clear`；业务侧可随时 `getUser()` |
| `TraceContext` | `ThreadLocal<String>` traceId，由 `TraceFilter` 写入 |

### 2.3 `entity/`

| 类 | 说明 |
|---|---|
| `BaseEntity` | 业务实体基类。`id`（`IdType.ASSIGN_ID` 雪花算法）、`createdBy / createdTime`（INSERT 填充）、`updatedBy / updatedTime`（UPDATE 填充）、`isDeleted`（逻辑删除，`@TableLogic(value="0", delval="1")`） |

### 2.4 `model/`

| 类 | 说明 |
|---|---|
| `ApiResponse<T>` | 统一响应 `{ code, message, data }`（**只有 getter，不可 set**）。静态工厂：`success()` / `success(data)` / `failure(ResultStatus)` / `failure(ResultStatus, message)` |
| `AuthUser` | 当前登录用户：`id` / `username` / `type`（`TYPE_USER` 或 `TYPE_MEMBER`），提供 `isMember()` |
| `PageResult<T>` | 分页响应包装：`records / total / current / size / pages / hasMore`。静态工厂：`of(IPage)` / `of(IPage, List<T>)` / `map(IPage, Function<S,T>)` / `empty(current,size)` |

### 2.5 `exception/`

| 类 | 说明 |
|---|---|
| `BizException` | 业务异常，构造参数 `ResultStatus status, String message` |
| `GlobalExceptionHandler` | 统一异常处理器（详见 §3.5） |

### 2.6 `enums/`

| 类 | 说明 |
|---|---|
| `ResultStatus` | 状态码枚举，含 `int code` + `String message` + `HttpStatus httpStatus`。分段约定：`0` 成功 / `1xxx` 通用 / `2xxx` 鉴权 / `3xxx` 业务 / `4xxx` 第三方 / `5xxx` 服务端内部 |

### 2.7 `utils/`

| 类 | 说明 |
|---|---|
| `TraceIdGenerator` | 生成 8 位 `a-zA-Z0-9` 随机 traceId（基于 `ThreadLocalRandom`） |
| `RandomUsernameGenerator` | 生成 `"用户" + 8 位字母` 形式的默认用户名（基于 `SecureRandom`） |

---

## 3. 集成指南

### 3.1 Maven 依赖

业务模块（以 `core` 为例）`pom.xml` 添加：

```xml
<dependency>
    <groupId>com.slm</groupId>
    <artifactId>common</artifactId>
    <version>${parent.version}</version>
</dependency>
```

common **不依赖任何业务模块**，业务模块**不应**反向依赖 common 之外的业务模块。

### 3.2 包扫描（必须显式指定）

Spring Boot 默认只扫描 `@SpringBootApplication` **当前包及其子包**。common 的类都在 `com.slm.common.*`，**不在启动类默认扫描范围内**。

二选一：

**方案 A（推荐）：启动类放在 `com.slm` 包下**

```
com.slm
├── common/...
└── barbershop/
        └── BarberShopApplication.java   ← 与 common 平级
```

**方案 B：显式声明扫描包**

```java
@SpringBootApplication(scanBasePackages = "com.slm")
public class BarberShopApplication { ... }
```

> ⚠️ **不扫描 → common 所有 `@Component` / `@Configuration` 都不会被注册**，`ApiLogAspect`、`TraceFilter`、`GlobalExceptionHandler` 等全部失效，且不会编译报错——这是最常见的集成踩坑点。

### 3.3 `application.yaml` 必填项

`SwaggerConfig` 通过 `@ConfigurationProperties(prefix = "springdoc-info")` 绑定元数据，必须配置：

```yaml
springdoc-info:
  title:          你的接口文档标题
  description:    你的接口文档描述
  version:        v1.0.x
  contact-name:   你的名字
```

### 3.4 日志配置（可选覆盖）

common 自带 `logback-spring.xml`，包含 traceId 输出格式（`[%traceId]`）、控制台彩色日志、按大小/时间滚动文件、自动分离 `error.log`（保留 90 天）等。

**优先级**：业务模块 `src/main/resources/logback-spring.xml` **会覆盖** common 的同名文件。如需保留 traceId 输出，自定义配置里记得注册 converter：

```xml
<conversionRule conversionWord="traceId"
                converterClass="com.slm.common.config.TraceIdConverter" />
```

并在 pattern 里使用 `[%traceId]`。

### 3.5 Mapper 扫描器（必须显式注册）

业务 `@SpringBootApplication` 必须显式 `@Import` 通配扫描器，否则 `com.slm.<module>.mapper` 下的 Mapper 接口不会被注册，启动期出现 `Invalid bound statement (not found)`：

```java
import com.slm.common.config.WildcardMapperScanRegistrar;

@Import(WildcardMapperScanRegistrar.class)
@SpringBootApplication(scanBasePackages = "com.slm")
public class BarberShopApplication { ... }
```

扫描器按 `com.slm.<module>.mapper` 路径通配匹配，因此**新增业务模块时只要把 Mapper 放到这个包下，无需修改任何配置**。

### 3.6 业务异常处理

`GlobalExceptionHandler` 已处理以下异常，业务侧**不需要**再声明 `@ExceptionHandler`：

| 异常 | HTTP | 状态码 |
|---|---|---|
| `BindException`（GET `@ModelAttribute`） | `BAD_REQUEST` | `1000 BAD_REQUEST` + 首个字段错误信息 |
| `MethodArgumentNotValidException`（`@RequestBody @Valid`） | `BAD_REQUEST` | 同上 |
| `ConstraintViolationException`（`@Validated` 直接注解参数） | `BAD_REQUEST` | 同上 |
| `MaxUploadSizeExceededException` | `PAYLOAD_TOO_LARGE` | `1003 FILE_SIZE_EXCEEDED` |
| `BizException` | 取自 `ResultStatus.httpStatus` | `ResultStatus.code` + `BizException.message` |
| `Exception`（兜底） | `INTERNAL_SERVER_ERROR` | `-1 ERROR` + `"服务器内部错误"` |

业务侧只需抛 `BizException`：

```java
throw new BizException(ResultStatus.SOURCE_NOT_FOUND, "门店不存在");
```

---

## 4. 使用说明

### 4.1 Controller 返回值

所有 Controller 方法**统一返回 `ApiResponse<T>`**：

```java
@GetMapping("/shop/{id}")
public ApiResponse<ShopResponse> get(@PathVariable Long id) {
    return ApiResponse.success(shopService.getById(id));
}

// 分页
@GetMapping("/shop/page")
public ApiResponse<PageResult<ShopResponse>> page(ShopQuery query) {
    IPage<Shop> page = shopService.page(query);
    return ApiResponse.success(PageResult.map(page, ShopConverter::toResponse));
}
```

### 4.2 分页查询协议

Controller 方法签名里直接声明 `Page<T>` 子类（或 `Page<T>`），前端传参：

| 参数 | 类型 | 说明 |
|---|---|---|
| `current` | long | 当前页，默认 1，最小 1 |
| `size` | long | 每页大小，默认 10，上限 1000（超出会被截断） |
| `searchCount` | boolean | 是否查询总数 |
| `sort` | string | 排序字段+方向，逗号分隔。支持 `?sort=name,asc&sort=created_time,desc` 或 `?sort=name,asc,created_time,desc` |

排序字段只能是实体类声明的字段（含主键），其他字段会被忽略。

### 4.3 实体继承

```java
@Data
@TableName("shop")
public class Shop extends BaseEntity {
    // 自己的业务字段
}
```

`BaseEntity` 已包含：

- `id`（雪花算法自动生成）
- `createdBy / createdTime`（insert 填充）
- `updatedBy / updatedTime`（update 填充）
- `isDeleted`（逻辑删除，所有查询自动加 `is_deleted = 0`）

**乐观锁**：`BaseEntity` **不含** `version` 字段，需要乐观锁的实体自己加：

```java
@TableName("shop")
public class Shop extends BaseEntity {
    @Version
    private Long version;
}
```

### 4.4 traceId 链路追踪

每个 HTTP 请求会自动：

1. 优先从请求 Header `X-Trace-Id` 读取；没有则 `TraceIdGenerator` 生成 8 位
2. 写入 `TraceContext`（ThreadLocal）
3. 回写到响应 Header `X-Trace-Id`，方便客户端联调
5. 在所有日志中以 `[traceId]` 输出（前提：logback 配置里 pattern 包含 `[%traceId]`）

业务侧取用：

```java
String traceId = TraceContext.getTrace();
```

### 4.5 当前登录用户

JWT 过滤器在请求开始时把当前用户写入 `UserContext`，业务侧直接：

```java
AuthUser user = UserContext.getUser();
if (user != null) {
    Long id = user.getId();
    String name = user.getUsername();
    boolean isMember = user.isMember();
}
```

`EntityMetadataConfig.insertFill` / `updateFill` 也是从这里读取 `user.getId()` 作为 `createdBy` / `updatedBy` 的值。

---

## 5. 注意事项（踩坑清单）

| # | 注意点 | 影响 |
|---|---|---|
| 1 | **必须显式 `scanBasePackages = "com.slm"`** 或启动类放 `com.slm` 包下，否则 common 全部 `@Component` 不会被扫描，**编译不报错** | 全部 Bean 失效 |
| 2 | **业务模块不要重复注册 `MybatisPlusInterceptor`** | 重复注册可能造成分页/乐观锁 SQL 被嵌套改写，行为难预测 |
| 3 | **业务模块不要再声明同名 `@RestControllerAdvice` / `@ExceptionHandler`** | 否则 common 的 `GlobalExceptionHandler` 会被覆盖，统一响应/统一状态码失效 |
| 4 | **`BaseEntity` 没有 `version` 字段** | 需要乐观锁的实体自己声明 `@Version Long version` |
| 5 | **`JsonConfig` 全局 `Long` 序列化为字符串** | 前端不需要 `BigInt` 即可无损拿到雪花 ID；下游若用 `Number` 类型反序列化需自行处理 |
| 6 | **`GlobalCorsConfig` 当前 `addAllowedOrigin("*")`** | 生产环境必须改为具体域名 |
| 7 | **`ApiLogAspect` 切点为 `com.slm..controller..*.*`** | 业务模块的 controller 必须落在 `com.slm` 子包下才能被拦截到；不在该包下会沉默失效 |
| 8 | **`PageHandlerMethodArgumentResolver` 排序字段必须是实体字段** | 非法字段会被静默忽略；如需按 VO 字段排序请在前端转好 |
| 9 | **`logback-spring.xml` 同名覆盖** | 业务模块有同名文件会整体覆盖 common 的；保留 traceId 必须显式注册 converter |
| 10 | **`UserContext` 必须配对 `clear()`** | Tomcat 线程复用，未 `clear` 会导致脏数据。**推荐由 Filter / Interceptor 统一管理 `set + clear`** |
| 11 | **`EntityMetadataConfig` 依赖 `UserContext.getUser()`** | 系统初始化、定时任务、外部回调等无登录态场景，`createdBy / updatedBy` 为 `null`；如需兜底请自行修改该类或在拦截器里预填 |
| 12 | **`ApiResponse` 是 `@Getter` 而非 `@Data`** | 不可 set，构造时通过静态工厂方法生成 |
| 13 | **`BizException.message` 会覆盖 `ResultStatus.message`** | 即 status.code 用枚举的，message 用 `BizException` 传入的，便于业务侧自定义提示 |
| 14 | **`@ConfigurationProperties(prefix = "springdoc-info")` 必须填** | Swagger 启动可能因缺字段失败/标题为空 |
| 15 | **`WildcardMapperScanRegistrar` 必须显式 `@Import`** | 不导入则 `com.slm.<module>.mapper` 全部 Mapper 不生效，启动报 `Invalid bound statement (not found)`，且编译期无报错 |

---

## 6. 依赖一览（common 自身）

| 依赖 | 用途 |
|---|---|
| `spring-boot-starter-web` | Web MVC |
| `spring-boot-starter-validation` | JSR-303 校验 |
| `spring-boot-starter-aop` | `ApiLogAspect` |
| `mybatis-plus-boot-starter` + `mybatis-plus-jsqlparser` | 分页/乐观锁/字段填充 |
| `springdoc-openapi-ui` | Swagger UI（OpenAPI 3） |
| `lombok` | `@Data` / `@Getter` 等 |
| `commons-lang3` | `StringUtils` |

**业务模块还需要的依赖**（不在 common 范围内）：`spring-boot-starter-mail` / `mysql-connector-j` / `jjwt` / `spring-security-crypto` / `spring-boot-starter-data-redis` / `redisson-spring-boot-starter` / `mapstruct` / `lombok-mapstruct-binding` / `minio` / `flyway-core` / `flyway-mysql` / `micrometer-registry-prometheus` 等，请按需在业务模块 `pom.xml` 中单独引入。