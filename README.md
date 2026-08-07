# Barbershop Service

理发店业务管理系统后端服务，提供门店、会员、交易、文件等核心能力的 RESTful 接口。

## 技术栈

| 模块        | 版本 / 选型                       |
| ----------- | --------------------------------- |
| JDK         | 11                                |
| 框架        | Spring Boot 2.7.18                |
| 持久层      | MyBatis-Plus 3.5.9                |
| 数据库      | MySQL 8.0 + Flyway 版本迁移       |
| 缓存 / 锁   | Redis + Redisson 3.24.3           |
| 对象存储    | MinIO 8.5.10                      |
| 鉴权        | JWT (jjwt 0.12.5)                 |
| 接口文档    | springdoc-openapi-ui 1.7.0        |
| 对象映射    | MapStruct 1.5.5 + Lombok          |
| 邮件        | Spring Boot Mail (SMTP)           |

## 目录结构

```
barbershop/
├── src/main/java/com/slm/barbershop/
│   ├── controller/        # Auth / Shop / Member / Appointment / Bill / ServiceCategory / ServiceItem / File
│   ├── service/           # 业务实现
│   ├── mapper/            # MyBatis-Plus Mapper
│   ├── entity/            # User / Shop / Member / MemberTransaction / Appointment / Bill / ServiceCategory / ServiceItem / FileMetadata
│   ├── model/             # DTO / VO
│   ├── converter/         # MapStruct 转换器
│   ├── config/            # Web / MyBatis-Plus / Redisson / MinIO 等配置
│   ├── lock/              # 分布式锁抽象（多模式）
│   ├── exception/         # 全局异常处理
│   ├── enums/             # 枚举
│   └── utils/             # 工具类
├── src/main/resources/
│   ├── application.yaml   # 主配置
│   ├── db/migration/      # Flyway SQL 迁移脚本
│   └── mapper/            # 自定义 XML
├── src/test/              # 单元测试
├── frontend/member-portal.html  # 会员门户单页应用（手机号登录、店铺切换、预约等）
├── docker/                # Docker 构建配置
├── docker-compose.yml     # MySQL / MinIO / Redis 一键启动
├── Dockerfile
└── pom.xml
```

## 快速开始

### 环境依赖

- JDK 11+
- Maven 3.8+
- Docker（可选，用于一键拉起 MySQL / MinIO / Redis）

### 启动基础服务

```bash
docker-compose up -d mysql minio redis
```

### 本地运行

```bash
# 默认配置：服务监听 8080，context-path = /barbershop
mvn spring-boot:run
```

### 打包部署

```bash
mvn clean package -DskipTests
java -jar target/barbershop.jar
```

### 接口文档

启动后访问：

```
http://localhost:8080/barbershop/swagger-ui.html
```

## 接口约定

统一响应结构：

```json
{ "code": 0, "message": "ok", "data": { /* 业务数据 */ } }
```

主要端点（context-path: `/barbershop`）：

| 模块       | 路径前缀             | 说明                          |
|----------|------------------|-----------------------------|
| 认证       | `/auth`          | 登录、刷新令牌等                   |
| 店铺       | `/shop`          | 店铺信息维护、营业时间、概览/统计接口        |
| 会员       | `/member`        | 会员信息与交易流水（含手机号登录）           |
| 预约       | `/appointment`   | 预约创建/分页查询/状态流转                |
| 账单       | `/bill`          | 独立账单系统（含 KPI 指标、散户/会员消费明细）   |
| 服务类目/项目 | `/service-category` `/service-item` | 服务类目与项目维护             |
| 文件       | `/file`          | MinIO 上传/下载                |

## 配置项（环境变量）

| 变量名                       | 默认值                                | 说明                       |
| ---------------------------- | ------------------------------------- | -------------------------- |
| `SERVER_PORT`                | `8080`                                | 监听端口                   |
| `SERVER_SERVLET_CONTEXT_PATH` | `/barbershop`                         | 服务前缀                   |
| `SPRING_DATASOURCE_URL`      | `jdbc:mysql://localhost:3306/barbershop` | 数据库连接                 |
| `SPRING_DATASOURCE_USERNAME` | `root`                                | 数据库用户                 |
| `SPRING_DATASOURCE_PASSWORD` | `123456`                              | 数据库密码                 |
| `REDIS_HOST` / `REDIS_PORT`  | `localhost` / `6379`                  | Redis 连接                 |
| `REDIS_PASSWORD`             | `redis123456`                         | Redis 密码                 |
| `MINIO_ENDPOINT`             | `http://localhost:9000`               | MinIO 服务地址             |
| `MINIO_ACCESS_KEY`           | `minioadmin`                          | MinIO 账号                 |
| `MINIO_SECRET_KEY`           | `minioadmin`                          | MinIO 密码                 |
| `MINIO_BUCKET`               | `barbershop`                          | 存储桶                     |
| `JWT_SECRET`                 | （32 字节 base64）                    | JWT 签名密钥               |
| `JWT_ACCESS_TOKEN_EXPIRATION` | `86400`                              | Access Token 过期（秒）    |
| `MAIL_HOST` / `MAIL_USERNAME` / `MAIL_PASSWORD` | 163 SMTP 默认值           | 邮件通知                   |

## 开发说明

- 数据库变更一律通过 [src/main/resources/db/migration/](src/main/resources/db/migration) 中的 Flyway 脚本管理，禁止直接改库
- 会员交易等高并发写操作使用 [src/main/java/com/slm/barbershop/lock/](src/main/java/com/slm/barbershop/lock) 中的分布式锁抽象（支持 Redisson 多模式）
- 全局异常统一在 `exception/` 处理，Controller 不应捕获业务异常
- DTO / VO 与 Entity 互转统一使用 `converter/` 下的 MapStruct

## 会员门户

会员门户前端位于 [frontend/member-portal.html](frontend/member-portal.html)，为单文件 SPA，无构建步骤，浏览器直接打开即可。

核心能力：
- 手机号验证码登录
- 店铺切换（支持多店会员体系）
- 服务项目浏览与预约
- 会员账单/消费明细查询
