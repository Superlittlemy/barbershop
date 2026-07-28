# 大盘 KPI 聚合接口需求文档

| 项目         | 值                                                          |
| ------------ | ----------------------------------------------------------- |
| 接口名称     | 获取概览页 KPI 聚合数据                                      |
| 接口代号     | `GET /shop/stats/overview`                                  |
| 版本         | v1.1（已对齐后端答复）                                      |
| 关联前端     | `barbershop-vue` —— `src/views/OverviewView.vue`           |
| 关联前端 Store | `src/stores/shop.js`                                       |
| 撰写人       | 前端 / 联合评审                                              |
| 状态         | §10 开放问题已全部确认，待后端进入开发排期                   |

> v1.1 相对 v1.0 的关键变更：`recentTxCount` → `recentTxAmount`，含义从「笔数」改为「近 30 天消费金额（元）」；其余答复见 §10。

---

## 1. 背景与现状

当前概览页（`/#/overview`）初始化时，前端需要为首页 4 个 KPI 卡片调用以下接口：

| # | 请求                                                         | 目的                         |
| - | ------------------------------------------------------------ | ---------------------------- |
| 1 | `GET /shop/page?page=1&size=999`                            | 拉店铺列表以统计店铺数       |
| 2 | `GET /member/page?shopId={id}` × N（每店 1 次）            | 取所有会员求会员总数与余额合计 |
| 3 | `GET /member/{memberId}/transactions` × N（有会员的店）     | 取每个店铺「第一个会员」的交易数作 `recentTxCount` |

在 N=3 的实测中：

- 总请求数：**10**
- 等待时间瓶颈：步骤 2、3 全部串行执行
- 业务正确性问题：`recentTxCount` 仅统计每店第一个会员的交易笔数，**无业务含义**
- 带宽浪费：为求聚合值，把全量会员数据传输到前端

随店铺数量增长（10、30、100），请求数线性放大为 `1 + 2N + N = 1 + 3N`，首屏耗时不可接受。

## 2. 目标与非目标

### 2.1 目标

1. 提供一个聚合接口，前端一次拿到首页所需的全部 KPI 数值。
2. 后端负责聚合，前端不再拉全量会员只为求和。
3. `recentTxAmount` 字段具备明确业务语义。
4. 接口耗时受店铺规模影响小（O(1) 而非 O(N)）。

### 2.2 非目标（不在本期范围）

- 不修改 `/shop/page`、`/member/page` 等既有接口的契约；如需扩展内联字段，另行单独立项。
- 不重做大盘的图表、趋势、对比维度。
- 不动鉴权方案，沿用现有 JWT / `Authorization` Header。
- 不支持时间窗口筛选（详见 §10 Q3）。

## 3. 接口契约

### 3.1 基本信息

| 项     | 值                                                                 |
| ------ | ------------------------------------------------------------------ |
| Method | `GET`                                                              |
| Path   | `/shop/stats/overview`                                              |
| 鉴权   | 必填 Header：`Authorization: Bearer <token>`                        |
| Content-Type | `application/json; charset=utf-8`                              |
| 时区   | 所有时间字段统一 **UTC（ISO 8601）** 返回；前端展示层自行转换本地时区 |
| 缓存   | 后端 Redis 缓存，TTL 30~60 秒（详见 §5）                          |

### 3.2 请求参数

**Query 参数**：无。

**Body**：无。

### 3.3 响应结构

成功响应（HTTP 200）：

```json
{
  "code": 20000,
  "message": "ok",
  "data": {
    "shopCount": 3,
    "memberCount": 312,
    "totalBalance": 128450.55,
    "recentTxAmount": 87342.10,
    "asOf": "2026-07-21T08:30:00Z"
  }
}
```

### 3.4 字段定义

| 字段            | 类型     | 单位 | 必返回 | 含义                                                                 |
| --------------- | -------- | ---- | ------ | -------------------------------------------------------------------- |
| `shopCount`     | integer  | 个   | ✔     | 当前登录用户可见的店铺总数                                            |
| `memberCount`   | integer  | 人   | ✔     | 所有店铺的会员总数；会员**不支持跨店共享**，故直接等于各店会员数之和   |
| `totalBalance`  | number   | 元   | ✔     | 所有会员的储值余额合计；保留 2 位小数；为 0 时返回 `0.00`           |
| `recentTxAmount`| number   | 元   | ✔     | 近 30 天**消费类**交易金额合计；**不含储值**；保留 2 位小数（详见 §4.1）|
| `asOf`          | string   | ISO 8601 | ✔ | 数据快照时间；前端可用于「实时更新」提示与本地缓存失效判断；同时承担「上次刷新时间」职责（详见 §10 Q6）|

### 3.5 错误响应

沿用项目现有统一错误格式：

```json
{
  "code": 40100,
  "message": "未登录或登录已过期",
  "data": null
}
```

| HTTP | code     | 含义                       | 处理建议                     |
| ---- | -------- | -------------------------- | ---------------------------- |
| 401  | 40100    | 未登录 / Token 失效         | 前端跳登录                   |
| 403  | 40300    | 无权限访问大盘             | 前端 toast 提示              |
| 500  | 50000    | 服务器内部错误             | 前端重试 + 上报              |
| 503  | 50300    | 下游依赖不可用（DB / Cache）| 前端降级为空态                |

## 4. 业务规则

### 4.1 `recentTxAmount` 定义（已对齐后端）

**口径：以 `asOf` 为截止点，回溯 30×24 小时内的「消费类」交易金额合计**。

- **包含**：交易类型 `type = consume`（消费）。
- **排除**：
  - 交易类型 `type = recharge`（储值 / 充值）。
  - 交易类型 `type = refund`（如有，也排除；如业务认为应计入，请回复确认）。
  - 被撤销、作废的记录（按业务已有的状态字段判定）。
- **时间字段**：以交易表 `created_at` 为准（具体字段名以后端模型为准）。
- **时区**：以服务器时区或 DB 时区为准；返回的 `asOf` 字段用作语义对齐。
- **精度**：保留 2 位小数；多笔累加按 §4.3 处理。

> 前端展示：仍使用首页 KPI 卡片 `最近交易` 槽位，**值会通过 `formatYuan()` 渲染为 `¥xx.xx`**，与 `储值总额` 卡片一致。

### 4.2 会员去重（已对齐后端）

**会员不支持跨店共享**，因此：

```
memberCount = SUM(各店会员数)
```

不需要按 `member.id` 做去重聚合，单条 `GROUP BY shop_id` 即可完成。

### 4.3 余额合计精度

- 内部计算用 `DECIMAL(18,2)` 或等效类型。
- 合计时**不要**做四舍五入后累加，应**先求和再保留 2 位**，避免误差。
- `recentTxAmount` 同理。

### 4.4 可见性

- 仅返回当前登录用户有权访问的店铺的统计值。
- 若用户无任何店铺，返回 `shopCount=0, memberCount=0, totalBalance=0, recentTxAmount=0`，**不要返回 4xx**。

## 5. 性能要求

| 指标                     | 目标值                                  |
| ------------------------ | --------------------------------------- |
| 单次响应 P50             | ≤ 80 ms                                  |
| 单次响应 P95             | ≤ 250 ms                                 |
| 并发 100 QPS             | P95 ≤ 500 ms                             |
| DB 查询次数（单次请求）  | ≤ 3（店铺 / 会员 / 交易 各自 1 次聚合） |
| 缓存层                   | **Redis**（已确认，详见 §10 Q5）         |
| 缓存 TTL                 | 30~60 秒（推荐 45 秒）                  |
| 缓存粒度                 | 按 `userId`（或租户）维度                |
| 缓存命中响应             | P95 ≤ 20 ms                              |

> 实现建议：
> - 店铺与会员聚合可单条 `GROUP BY shop_id` SQL 解决。
> - `recentTxAmount` 可用 `SUM(amount) WHERE type='consume' AND created_at >= ?` 一次性算出，配合索引 `(type, created_at)` 或 `(shop_id, type, created_at)`。
> - Redis Key 建议：`shop:stats:overview:user:{userId}`；写入时同步刷新 `asOf`，便于排查缓存漂移。

## 6. 与现有接口的关系

| 接口                            | 关系                                                          |
| ------------------------------- | ------------------------------------------------------------- |
| `GET /shop/page`                | 不变；后续可在每条 record 内联 `stats`，但本期不动              |
| `GET /member/page?shopId=`     | 不变；本期不动                                                |
| `GET /member/{id}/transactions`| 不变；本期不再用于概览页                                      |
| `GET /shop/{id}/transactions/recent` | 不变；详情页使用                                          |

## 7. 兼容性与迁移

### 7.1 发布顺序

1. 后端先发布本接口（`/shop/stats/overview`）。
2. 前端在 `stores/shop.js` 中**先并行调用**新旧逻辑，新接口上线即用，否则 fallback 到旧逻辑。
3. 观察 1~2 周后，下掉旧的客户端聚合代码。

### 7.2 灰度方案（可选）

- 通过 Header `X-Feature-Shop-Stats-Overview: on` 控制是否走新接口。
- 后端按租户/用户 ID 灰度开放。

### 7.3 字段重命名兼容

`recentTxCount` → `recentTxAmount` 是**破坏性变更**。如需平滑过渡，可考虑：

- 方案 A（推荐）：直接以 `recentTxAmount` 为正式字段上线；前端 v1.1 一并发布。
- 方案 B：双发期同时返回 `recentTxCount`（旧，标注 deprecated）与 `recentTxAmount`（新），前端过渡完下线旧字段。

## 8. 测试用例

| #  | 场景                              | 期望                                                                 |
| -- | --------------------------------- | -------------------------------------------------------------------- |
| 1  | 用户有 3 个店铺、312 个会员        | 返回正确 `shopCount/memberCount/totalBalance`                        |
| 2  | 用户无任何店铺                    | 返回全 0，不报错                                                    |
| 3  | 未登录请求                        | HTTP 401，code=40100                                                |
| 4  | Token 过期                        | HTTP 401，code=40100                                                |
| 5  | 数据库中无 30 天内消费交易         | `recentTxAmount=0`                                                  |
| 6  | 会员余额含 0.001、99.999 等边界值 | `totalBalance` 精度正确（保留 2 位，舍入正确）                       |
| 7  | 仅含储值，无消费交易              | `recentTxAmount=0`（已确认排除储值）                                  |
| 8  | 同时含消费与储值                  | `recentTxAmount` 仅累加消费部分                                       |
| 9  | 含撤销 / 作废的消费记录           | `recentTxAmount` 不计入被撤销的部分                                   |
| 10 | 会员跨店重复                      | 不适用（业务已确认不支持跨店共享）                                    |
| 11 | 服务降级（DB 不可用）              | HTTP 503，code=50300                                                |
| 12 | Redis 缓存命中                    | 响应 P95 ≤ 20 ms                                                     |
| 13 | Redis 缓存未命中                  | 走 DB 聚合，P95 ≤ 250 ms                                              |
| 14 | 并发 100 QPS 压测                  | P95 ≤ 500 ms，错误率 < 0.1%                                         |
| 15 | 时区切换测试                      | `asOf` 始终返回 UTC                                                  |

## 9. 交付物

- [ ] 接口实现（含单元测试与集成测试）
- [ ] 接口文档更新（OpenAPI/Swagger / Apifox / YApi 等）
- [ ] Postman / Apifox 测试集合（覆盖 §8 用例）
- [ ] Redis 缓存配置（含 Key 规范、TTL、降级策略）
- [ ] 灰度开关（如启用灰度）
- [ ] 变更日志条目

## 10. 开放问题答复记录

> v1.1 起所有开放问题已确认；本节作为**决议记录**留存，便于后续追溯。

| # | 问题                                                  | 答复                                                                 | 文档影响 |
| - | ----------------------------------------------------- | -------------------------------------------------------------------- | -------- |
| 1 | `recentTxCount` 的口径是否同意「近 7 天所有交易类型」？ | **否**。改为：**近 30 天消费类交易金额合计**，不含储值。字段同步重命名为 `recentTxAmount` | §3.3 / §3.4 / §4.1 / §7.3 |
| 2 | 会员是否跨店共享？                                    | **否**。`memberCount = SUM(各店会员数)`，无需去重                       | §4.2     |
| 3 | 大盘统计是否需要支持时间范围筛选？                     | **否**。保持无 Query 参数                                              | §2.2 / §3.2 |
| 4 | 是否需要「活跃店铺数」字段？                          | **否**。沿用 `shopCount` 一个字段即可                                  | §3.4（无新增字段） |
| 5 | 缓存层是否走 Redis？TTL 多少？                        | **是**。TTL 推荐 30~60 秒（建议 45 秒），按 `userId` 维度              | §3.1 / §5 / §8 / §9 |
| 6 | 是否需要「上次刷新时间」字段？                         | **否**。沿用 `asOf` 即可                                              | §3.4（无新增字段） |

### 10.1 仍待后端最终确认的小点

1. `recentTxAmount` 是否**也排除** `refund` 类型交易？当前默认排除；如需计入，请回复。
2. `recentTxAmount` 的时间字段是 `created_at` 还是 `paid_at` / `settled_at`？请告知具体字段名。
3. Redis Key 命名是否同意 `shop:stats:overview:user:{userId}`？如项目已有命名规范，请告知。

---

## 附录 A：前端预期调用方式（参考）

```js
// src/api/shop.js
export async function getOverviewStats() {
  const r = await http.get('/shop/stats/overview')
  return r.data
}
```

```js
// src/stores/shop.js —— 适配 v1.1
async loadAll() {
  if (this._loading) return this._loading
  this._loading = (async () => {
    const [shopRes, ovRes] = await Promise.all([
      listShops(),
      getOverviewStats(),                  // 新接口
    ])
    if (shopRes.code === 20000) {
      this.list = (shopRes.data && shopRes.data.records) || []
    }
    if (ovRes.code === 20000 && ovRes.data) {
      const d = ovRes.data
      this.overview = {
        shopCount: d.shopCount ?? 0,
        memberCount: d.memberCount ?? 0,
        totalBalance: d.totalBalance ?? 0,
        recentTxAmount: d.recentTxAmount ?? 0,  // 注意：原字段 recentTxCount 已重命名
        asOf: d.asOf ?? null,
      }
    }
    this.loaded = true
  })().finally(() => { this._loading = null })
  return this._loading
}
```

```vue
<!-- src/views/OverviewView.vue —— v1.1 适配 -->
<KpiCard label="最近交易" :value="formatYuan(shop.overview.recentTxAmount)" trend="实时更新" />
```

## 附录 B：变更历史

| 版本 | 日期       | 变更人 | 描述                                                                 |
| ---- | ---------- | ------ | -------------------------------------------------------------------- |
| v1.0 | 2026-07-21 | 前端   | 草案                                                                 |
| v1.1 | 2026-07-21 | 前端   | §10 全部 6 个开放问题已对齐：字段 `recentTxCount` → `recentTxAmount`（含义由笔数改为金额，30 天消费、不含储值）；缓存确认走 Redis（TTL 30~60s）；其余见 §10 |
