# 会员门户 (member-portal.html) 后端对接

**日期**: 2026-06-24
**类型**: 前端 + 后端 (新增接口)
**范围**: 1 个前端文件 + 后端 3 个新接口 + 少量配套代码

## 背景

`frontend/member-portal.html` 目前是纯静态展示，所有数据为硬编码占位（姓名"李明"、手机"138\*\*\*\*8888"、余额 888.00、最近消费"2024-03-15"）。后端已有完整的会员与交易能力（`Member` / `MemberTransaction` 实体，`/member/{id}`、`/member/{id}/balance`、`/member/{id}/transactions` 接口），但当前没有面向 C 端会员的登录入口，会员无法自助查询自己的资料与流水。

本次改动为会员门户打通以下三件事：

1. 会员通过"手机号 + 姓名"登录（不是后台系统用户登录，不复用 `/auth/*`）。
2. 登录后由接口数据回填页面（头像首字、姓名、手机号、店铺徽章、余额、最近消费时间）。
3. 提供分页加载的交易流水查询页（点击"消费记录"菜单触发）。

## 目标

1. **登录**：会员在前端输入手机号 + 姓名 → 校验通过后获得 JWT 与会员上下文 → 关闭登录蒙层 → 进入主页面。
2. **数据回填**：登录成功后依次拉取会员详情、余额、流水首页，渲染到现有 DOM 节点上。
3. **消费记录**：在弹窗中分页展示交易流水，支持"加载更多"。

## 非目标

- 不实现短信验证码、密码、第三方登录。
- 不实现会员注册（仅"老会员查询/登录"场景，新会员由后台系统管理员录入）。
- 不实现储值 / 消费 / 预约落库（仅前端"立即预约理发"按钮保留 alert 占位）。
- 不动任何存量接口路径与签名；新接口走新路径、新 DTO。
- 不改 `frontend/index.html`（控制台端，与本次 C 端独立）。
- 不动 `Member`、`MemberTransaction` 实体字段与表结构（不改数据库迁移）。

## 范围边界

| 范畴 | 范围 |
|---|---|
| 修改文件 | `frontend/member-portal.html`、`controller/MemberController.java`、`service/MemberService.java`、`utils/UserContext.java`、`config/WebMvcConfig.java`（或 `AuthInterceptor`） |
| 新增文件 | `model/MemberMatchRequest.java`、`model/MemberLoginRequest.java`、`model/MemberMatchVO.java`、`model/MemberTransactionPageRequest.java`、`service/MemberAuthService.java`、`config/MemberAuthInterceptor.java`（如需） |
| 不动 | 现有 `/auth/*`、`/member/{id}`、`/member/{id}/balance`、`/member/{id}/transactions`、`Member` / `MemberTransaction` 表结构 |

---

## 后端设计

### 1. 接口一：`GET /member/login/match`

**用途**：按 `phone` + `name` 跨店铺查找会员，返回所有匹配项（含店铺名）。

**请求**：
```
GET /barbershop/member/login/match?phone=13800008888&name=李明
```

**Query 参数**：
- `phone` (String, 必填)：11 位手机号
- `name` (String, 必填)：姓名（精确匹配）

**响应**：
```json
{
  "code": 0,
  "message": "ok",
  "data": [
    { "memberId": 1, "shopId": 1, "shopName": "星光理发店", "name": "李明", "phone": "13800008888" },
    { "memberId": 7, "shopId": 2, "shopName": "陆家嘴店",   "name": "李明", "phone": "13800008888" }
  ]
}
```

**校验规则**：
- `phone` 非空且长度 = 11
- `name` 非空
- 跨 `shop` 表 LEFT JOIN `member` 表，匹配 `m.phone = ? AND m.name = ? AND m.is_deleted = 0 AND s.is_deleted = 0`
- 命中 0 条 → `BizException(404, "未找到匹配的会员")`
- 命中 N 条 → 返回 N 条（前端按此决定是否走"店铺选择"分支）

**为什么不用 `/member/list?shopId=...`**：当前接口必须传 shopId，但前端登录时尚不知 shopId，必须支持无 shopId 跨店铺查询。

### 2. 接口二：`POST /member/login`

**用途**：在已知 shopId 的前提下完成最终登录，签发 JWT。

**请求**：
```
POST /barbershop/member/login
Content-Type: application/json

{ "phone": "13800008888", "name": "李明", "shopId": 1 }
```

**响应**：
```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "id": 1,
    "username": "李明"
  }
}
```

**校验规则**：
- `phone`、`name`、`shopId` 全部非空
- 查 `(shopId, phone, name, is_deleted=0)` 命中唯一一条
- 命中 → 调用 `JwtService.generate(memberId, "member")`，返回 `LoginResponse`
- 未命中 → `BizException(401, "姓名或手机号不正确")`

**JWT 主题（subject）**：`member:{memberId}`，与现有后台用户（`subject = userId`）区分。

### 3. 接口三：`GET /member/{id}/transactions/page`

**用途**：分页查询交易流水，前端"加载更多"使用。

**请求**：
```
GET /barbershop/member/1/transactions/page?page=1&size=20
```

**Query 参数**：
- `page` (int, 默认 1)：从 1 开始
- `size` (int, 默认 20，最大 100)

**响应**：
```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "records": [ /* MemberTransactionResponse[] */ ],
    "total": 53,
    "page": 1,
    "size": 20,
    "hasMore": true
  }
}
```

**排序**：`created_time DESC, id DESC`。

**鉴权**：要求 token，且 `subject` 中 memberId 与 path 中 `{id}` 一致；否则 403。

### 4. JWT 与上下文

**签发**：`JwtService.generate(memberId, "member")`，沿用现有 `JwtService`，仅 `subject` 拼接前缀区分身份类型。

**解析**：
- 在 `AuthInterceptor` 或新建 `MemberAuthInterceptor` 中，读取 `Authorization: Bearer xxx`
- 解析后判断 `subject` 前缀：
  - 以 `user:` 开头 → 解析为后台用户，走 `UserContext.setUser(...)`（现状）
  - 以 `member:` 开头 → 解析为会员，写入 `UserContext` 或新增 `MemberContext`

**复用策略**：为最小化扩散，复用 `UserContext` 但增加 `userType` 字段：
```java
public class AuthUser {
    private Long id;
    private String username;
    private String type;  // "user" 或 "member"，新增字段
}
```
鉴权注解（如未来引入）按 `type` 区分；当前阶段不上注解，靠 controller 内部判 `type` 即可。

### 5. Service 层

**`MemberService.match(phone, name)`**：
```java
public List<MemberMatchVO> match(String phone, String name) {
    return memberMapper.matchAcrossShops(phone, name);
}
```
- 在 `MemberMapper.xml` 中新增 SQL（或在 `MemberMapper` interface 用 `@Select` 注解）：
  ```sql
  SELECT m.id AS member_id, m.shop_id, s.name AS shop_name, m.name, m.phone
  FROM member m
  INNER JOIN shop s ON s.id = m.shop_id AND s.is_deleted = 0
  WHERE m.phone = #{phone}
    AND m.name  = #{name}
    AND m.is_deleted = 0
  ORDER BY m.id ASC
  ```

**`MemberAuthService.login(phone, name, shopId)`**：
```java
public LoginResponse login(String phone, String name, Long shopId) {
    Member m = memberMapper.selectOne(
        new LambdaQueryWrapper<Member>()
            .eq(Member::getShopId, shopId)
            .eq(Member::getPhone, phone)
            .eq(Member::getName, name)
            .eq(Member::getIsDeleted, 0)
    );
    if (m == null) throw new BizException(HttpStatus.UNAUTHORIZED, "姓名或手机号不正确");
    String token = jwtService.generate("member:" + m.getId(), m.getName());
    return new LoginResponse(token, m.getId(), m.getName());
}
```

**`MemberTransactionService.pageByMemberId(memberId, page, size)`**：
- 复用现有 `listByMemberId`，改为 `Page` 入参，调用 `memberTransactionMapper.selectPage(page, wrapper)`，返回自定义 `MemberTransactionPageVO`（不直接返回 MP 的 `IPage`，避免序列化细节）。

### 6. 配置

- **CORS**：检查现有 `WebMvcConfig`/`CorsConfig`。`member-portal.html` 是 file:// 直接打开，所有 `Origin` 是 `null`，需后端允许 `*` 或 `null`。**实际改动时确认现状后再决定是新增还是复用。**
- **拦截顺序**：`AuthInterceptor` 已存在；新增"member 类型识别"分支而不新增拦截器。

---

## 前端设计

### 1. 状态机

```
[Unauthenticated] --login success--> [Authenticated]
[Authenticated] --logout/401--------> [Unauthenticated]
```

登录态来源：`localStorage.member_token`。页面加载时检查：
- 有 token → 调 `/member/login/match` 不再需要，直接调 `/member/{id}` 拿当前 memberId（memberId 单独存 `localStorage.member_id`）
- 无 token → 显示登录蒙层

**注意**：token 中的 memberId 是 `subject`，前端不解析 JWT payload，单独存 memberId 到 `localStorage.member_id`。

### 2. 登录蒙层结构

```html
<div id="loginOverlay" class="overlay">
  <div class="login-card">
    <h2>会员登录</h2>
    <input id="loginPhone" placeholder="手机号" maxlength="11" />
    <input id="loginName"  placeholder="姓名"   maxlength="20" />
    <button id="loginSubmit">登录</button>
    <div id="loginError" class="error"></div>
    <!-- 当 match 返回多条时插入店铺选择 -->
    <div id="shopPicker" style="display:none">
      <p>检测到您在多个店铺有会员，请选择：</p>
      <div id="shopList"></div>
    </div>
  </div>
</div>
```

**样式**：沿用现有 `.modal` 的紫色渐变 + 圆角卡片视觉，不引入新设计语言。

### 3. 流程

```
onLoad
  ├─ token 存在 → fetchProfile() 直接进主页
  └─ token 不存在 → 显示 loginOverlay

点击"登录"
  └─ GET /member/login/match?phone=&name=
       ├─ 0 条  → 错误提示"未找到匹配的会员"
       ├─ 1 条  → 直接调 POST /member/login(phone, name, shopId)
       └─ N 条  → 渲染 shopPicker，用户点击店铺后调 POST /member/login

POST /member/login 成功
  ├─ localStorage.member_token   = res.data.token
  ├─ localStorage.member_id      = res.data.id
  ├─ localStorage.member_name    = res.data.username
  ├─ localStorage.member_shop_id = 当前店铺 ID  ← 新增：登录时由前端从 match 结果传入
  ├─ 隐藏 loginOverlay
  └─ fetchProfile()

fetchProfile()
  ├─ GET /member/{id}?shopId={member_shop_id}   → 渲染姓名、手机号、店铺徽章
  ├─ GET /member/{id}/balance                   → 渲染余额
  └─ GET /member/{id}/transactions/page?page=1&size=5
       └─ 取第一条 createdTime 渲染"最近消费: YYYY-MM-DD"
        （无交易时显示"暂无消费"）

切换店铺：调用 logout() 清掉 member_shop_id，重新走登录蒙层；在 match 阶段重新选择。

点击"消费记录"
  └─ 打开 txModal
       ├─ 已加载首页 20 条 → 直接渲染
       └─ 点击"加载更多" → GET .../page?page=N 追加到列表
```

### 4. fetch 封装

```js
async function apiCall(url, method, body) {
    const headers = { 'Content-Type': 'application/json' };
    const token = localStorage.getItem('member_token');
    if (token) headers['Authorization'] = `Bearer ${token}`;
    const res = await fetch(`/barbershop${url}`, {
        method,
        headers,
        body: body ? JSON.stringify(body) : undefined,
    });
    const json = await res.json();
    if (json.code === 401) { logout(); throw new Error(json.message); }
    if (json.code !== 0)   { throw new Error(json.message); }
    return json.data;
}
```

**注意**：当前前端可能没有"退出"按钮。最简策略：401 直接清 token + 刷新页面到登录蒙层。

### 5. DOM 渲染点

| 现有节点 | 渲染内容 |
|---|---|
| `.avatar` | `name.charAt(0)`（中文保留原字，英文 `toUpperCase()`） |
| `.profile-info h1` | 接口返回的 `name` |
| `.profile-info p` | 脱敏手机号 `${phone.slice(0,3)}****${phone.slice(7)}` |
| `.store-badge` | `${shopName} · 会员` |
| `.balance-amount` | `Number(balance).toLocaleString('zh-CN', { minimumFractionDigits: 2 })` |
| `.balance-hint` | 最近消费日期或"暂无消费" |

### 6. 交易记录 Modal

```html
<div id="txModal" class="modal">
  <div class="modal-content">
    <div class="modal-header">
      <span class="modal-title">消费记录</span>
      <div class="modal-close" onclick="closeTxModal()">✕</div>
    </div>
    <div id="txList"></div>
    <button id="loadMoreBtn">加载更多</button>
  </div>
</div>
```

**列表项**：
```
2024-03-15  消费  -¥88.00        余额 200.00
2024-02-20  储值  +¥500.00       余额 288.00
```

**加载更多**：点击后 `page++` 请求下一页，append 到列表。`hasMore=false` 时按钮变 "已加载全部" 并禁用。

---

## 数据流图

```
用户输入手机号+姓名
        │
        ▼
GET /member/login/match
        │
   ┌────┴────┐
   │ 0 条    1 条         N 条
   │   │     └→ POST /member/login ─→ token
   │   │                                │
   │   └→ 错误提示"未找到匹配的会员"      │
   │                                     ▼
   └→ 渲染店铺选择,用户点击            localStorage
                                        │
                                        ▼
                              GET /member/{id}
                              GET /member/{id}/balance
                              GET /member/{id}/transactions/page?page=1
                                        │
                                        ▼
                                    渲染页面
```

## 错误处理

| 场景 | 处理 |
|---|---|
| match 返回 0 条 | 登录卡内 toast "未找到匹配的会员，请联系门店" |
| match 返回 N 条未选择 | "登录"按钮禁用直到选择店铺 |
| login 401 | 错误提示"姓名或手机号不正确" |
| 网络异常 | catch 块 toast "网络异常，请重试"，按钮恢复可点 |
| token 过期（任意接口 401） | 清 localStorage，刷新页面回到登录蒙层 |
| `crypto.randomUUID` 不可用 | 降级到 `Date.now()-random`（仅 `/transactions/page` 不需要，本场景用不到，但保留 fetch 通用降级） |
| `Number.prototype.toLocaleString` 在极旧浏览器异常 | 降级 `String(balance)` |

## 验证

手工测试覆盖：

1. **首次登录（唯一匹配）**：输入匹配会员的手机号 + 姓名 → 进入主页，数据正确渲染。
2. **首次登录（多店匹配）**：输入同手机号 + 姓名 → 显示店铺选择 → 选店 → 进入对应店铺的主页。
3. **首次登录（无匹配）**：输入不存在 → 提示"未找到匹配的会员"。
4. **二次访问**：登录成功后刷新页面 → 自动进入主页（token 持久化生效）。
5. **token 过期**：手动改 localStorage 中的 token 为无效值 → 任意接口触发 401 → 自动回到登录页。
6. **消费记录加载**：交易数 < 20 → 直接展示，无加载更多；交易数 ≥ 20 → 翻页追加，hasMore=false 时按钮禁用。
7. **退出登录**（如确认加按钮）：清 localStorage + 刷新。

## 风险与回退

- **CORS 未配置**：`member-portal.html` 是 file:// 打开，跨域需后端 CORS 允许 `*` 或 `null`。验证阶段若失败，新增/调整 `WebMvcConfig.cors()`。
- **JWT subject 改造**：现有 `AuthInterceptor` 假设 subject 是 Long userId；新增 `member:` 前缀后必须同时兼容。改动时新增分支解析，不影响存量用户。
- **AuthUser 字段新增**：增加 `type` 字段会改 `AuthUser` 序列化输出，但现有控制台前端不使用该字段，零影响。
- **数据无隔离风险**：会员只能查自己（`memberId` 由 token 决定，path 中 `{id}` 必须与 token 中一致；后端在 controller 加 assert 即可）。
- **回退**：`git revert` 即可。新增文件不破坏既有逻辑。

## 决策记录

| 决策 | 取舍 |
|---|---|
| 不复用 `/auth/*`，新增 `/member/login` | 语义清晰，会员和管理员走不同入口；不复用避免影响现有控制台登录链路 |
| match 单独成接口而非直接登录时多步验证 | 一步到位需要传入 shopId 列表，前端需要先知道有几家店；分两步更自然 |
| 复用 JWT | 现有 `JwtService` 足够，只改 subject 拼接方式 |
| 复用 `AuthUser` 加 `type` 字段 | 避免新建 `MemberUser` 类扩散 |
| 不做分页时的"无交易"特殊页 | 空列表复用统一空状态（"暂无消费记录"） |
| 登录态用 localStorage | 简单场景，无需服务端 session；接受 XSS 风险（这是 C 端 demo） |