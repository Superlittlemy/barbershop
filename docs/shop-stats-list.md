# 店铺列表 + 每店聚合 stats 接口需求文档

| 项目         | 值                                                          |
| ------------ | ----------------------------------------------------------- |
| 接口名称     | 获取店铺列表（含每店聚合统计）                              |
| 接口代号     | `GET /shop/stats/list`                                      |
| 版本         | v1.0（首版草案）                                            |
| 关联前端     | `barbershop-vue` —— `src/views/OverviewView.vue`、`src/views/ShopsView.vue` |
| 关联前端 Store | `src/stores/shop.js`                                       |
| 撰写人       | 前端                                                        |
| 状态         | 待后端评审                                                  |

---

## 1. 背景与现状

当前店铺列表卡片（概览页 / 店铺管理页 / Sidebar）初始化时，前端需要发以下请求：

| # | 请求                                                         | 目的                           |
| - | ------------------------------------------------------------ | ------------------------------ |
| 1 | `GET /shop/page?page=1&size=999`                            | 拉店铺列表                     |
| 2 | `GET /member/page?shopId={id}` × N（每店 1 次，size=999）  | 取所有会员求 memberCount / totalBalance |

实测 N=3 个店铺：

- 总请求数：**4**（1 个 listShops + 3 个 listMembers）
- 等待瓶颈：步骤 2 全部串行执行（前端已并发化但带宽仍 N 倍）
- 带宽浪费：为求 memberCount / totalBalance，把全量会员数据传输到前端
- 业务正确性：`totalBalance` 把已删除 / 已转让会员的余额也一并计入

随店铺数量增长（10、30、100），请求数和带宽线性放大，列表首屏耗时不可接受。

## 2. 目标与非目标

### 2.1 目标

1. 提供一个聚合接口，一次返回店铺列表 + 每店聚合 stats。
2. 完全替代前端 `listShops()` + `loadShopStats()` 的组合调用。
3. 保持与 `/shop/page` 兼容的响应外壳，前端可平滑迁移。

### 2.2 非目标

- 不替代 `/shop/stats/overview`（大盘 KPI 仍走该接口）。
- 不替代 `/shop/{id}/transactions/recent`（详情页最近交易流）。
- 不替代 `/member/page`（详情页会员列表）。
- 不实现分页加载（一次返回全量 ≤ 999 即可，前端首页用 size=999 拉全）。

## 3. 接口契约

### 3.1 请求

```
GET /shop/stats/list?page=1&size=999
Authorization: <jwt>
```

| 参数  | 类型 | 必填 | 默认 | 说明                                   |
| ----- | ---- | ---- | ---- | -------------------------------------- |
| page  | int  | 否   | 1    | 分页页码（与 `/shop/page` 保持一致）   |
| size  | int  | 否   | 999  | 每页条数（首页一次拉全）               |

权限：登录用户，仅返回 `userId` 维度下的店铺 + 聚合。

### 3.2 响应

```json
{
  "code": 20000,
  "message": "ok",
  "data": {
    "records": [
      {
        "id": "shop_001",
        "name": "旗舰店",
        "logo": "https://cdn.example.com/shop/001.png",
        "address": "上海市黄浦区南京东路 100 号",
        "phone": "021-12345678",
        "businessHours": "09:00-21:00",
        "createdAt": "2026-01-15T08:30:00Z",
        "memberCount": 128,
        "totalBalance": 58234.50
      },
      {
        "id": "shop_002",
        "name": "分店 B",
        "logo": "",
        "address": "上海市浦东新区张江路 50 号",
        "phone": "021-87654321",
        "businessHours": "10:00-22:00",
        "createdAt": "2026-03-02T11:00:00Z",
        "memberCount": 42,
        "totalBalance": 12340.00
      }
    ],
    "total": 2
  }
}
```

### 3.3 字段定义

| 字段            | 类型            | 必返 | 说明                                                |
| --------------- | --------------- | ---- | --------------------------------------------------- |
| `id`            | string          | 是   | 店铺 ID，前端用 `String(id)` 做 map key            |
| `name`          | string          | 是   | 店铺名称                                            |
| `logo`          | string          | 否   | logo URL，空字符串表示无 logo                       |
| `address`       | string          | 否   | 地址                                                |
| `phone`         | string          | 否   | 电话                                                |
| `businessHours` | string          | 否   | 营业时间，自由文本                                  |
| `createdAt`     | string (ISO8601) | 否   | 创建时间                                            |
| `memberCount`   | number (int)    | 是   | 该店铺下未删除会员数量（按 `deleted=0` 过滤）        |
| `totalBalance`  | number (decimal)| 是   | 该店铺下未删除会员的储值余额合计，单位元，保留 2 位  |

### 3.4 与 `/shop/page` 的差异

唯一差异：每个 record 额外多 2 个聚合字段 `memberCount`、`totalBalance`。其余字段（含外壳 `{ code, data: { records, total } }`）与 `/shop/page` 完全一致，前端可在不破坏响应解析的前提下平滑切换。

## 4. 后端实现要点

### 4.1 单条 SQL（推荐）

```sql
SELECT
  s.id,
  s.name,
  s.logo,
  s.address,
  s.phone,
  s.business_hours   AS businessHours,
  s.created_at       AS createdAt,
  COUNT(m.id)        AS memberCount,
  COALESCE(SUM(m.balance), 0) AS totalBalance
FROM shop s
LEFT JOIN member m
       ON m.shop_id = s.id
      AND m.deleted = 0
WHERE s.deleted = 0
  AND s.user_id = :userId
GROUP BY s.id
ORDER BY s.created_at DESC
LIMIT :size OFFSET :offset;
```

注意：

- `LEFT JOIN` 保留无会员的店铺（memberCount=0, totalBalance=0）。
- `m.deleted = 0` 过滤掉已删除会员，避免 totalBalance 多算。
- 索引建议：`shop(user_id, deleted, created_at)`、`member(shop_id, deleted)`。

### 4.2 缓存

**本期不引入缓存**（已确认决策：先直查数据库，观察一段时间后再评估是否上 Redis）。二期再启用时建议：

- 与 `/shop/stats/overview` 共用 `userId:{uid}:shop-stats:v1` 键前缀。
- TTL 30~60s（Redis），写操作（创建/编辑/删除店铺、增删改会员）触发失效。
- 灰度期间建议同时保留 `/shop/page`，前端 fallback 仍可调用。

### 4.3 性能基线

- 店铺数 100、会员总数 5000 时，单条 SQL 在 **300ms 以内** 返回（建议后端做慢查询埋点监控，本期无缓存）。
- 二期加缓存后，缓存命中应在 **50ms 以内** 返回。

## 5. 前端改动

### 5.1 API 层（已落地）

[src/api/shop.js](../../src/api/shop.js) 新增 `listShopStats(page, size)`，保留 `listShops` 作为 fallback。

### 5.2 Store 层（已落地）

[src/stores/shop.js](../../src/stores/shop.js) 改造点：

- `loadAll()` 主路径用 `listShopStats()` 替代 `listShops() + loadShopStats()` 的两次调用 → 一次。
- `_ingestShopStats(records)` 从同一响应里拆出 `stats[id]`。
- `loadShopStatsLegacy()` 改并发 + 限流（concurrency=6），仅在 fallback 时触发。
- `loadOverviewStatsLegacy()` 改为从 `this.stats` 聚合，0 网络请求。
- `editShop` / `removeShop` 改为局部刷新，不触发整轮 `loadAll()`。
- 新增 `refreshShopStat(shopId)`，供详情页编辑/添加会员后调用。

### 5.3 收益估算

| 场景                  | 改造前请求数              | 改造后请求数         |
| --------------------- | ------------------------- | -------------------- |
| 概览页首次加载（N=10）| 1 listShops + 10 listMembers = **11** | 1 listShopStats + 1 overview = **2** |
| 概览页首次加载（N=100）| **101**                  | **2**                |
| 编辑店铺              | 1 updateShop + 1 listShops + 10 listMembers + 1 overview = **13** | 1 updateShop + 1 refreshShopStat = **2** |

## 6. 灰度与回滚

1. **阶段一**：后端实现 + 单元测试，前端并行接入但不切流量（双跑）。
2. **阶段二**：前端 `loadAll()` 切换主路径到 `listShopStats`，保留 `loadShopStatsLegacy` 作为 fallback。
3. **阶段三**：观察 1~2 周后，下线 `loadShopStatsLegacy` 与 `loadOverviewStatsLegacy` 的 fallback 代码路径。

回滚条件：`/shop/stats/list` P99 延迟 > 500ms 或 5xx 率 > 0.5%，前端将 `loadAll()` 主路径切回 `listShops() + loadShopStats()` 即可。

## 7. 决策记录

| # | 问题                                                                      | 结论                              |
| - | ------------------------------------------------------------------------- | --------------------------------- |
| 1 | `memberCount` 是否排除「已禁用」会员？                                  | **不区分**——当前业务无禁用功能    |
| 2 | `totalBalance` 是否包含「冻结金额」？                                   | **不包含**——当前业务无冻结概念    |
| 3 | 是否追加 `txCount30d` / `txAmount30d` 字段替代 overview？                | **不追加**，继续走 `/shop/stats/overview` |
| 4 | 是否上 Redis 缓存？                                                     | **本期不上**，直查数据库，二期再评估 |