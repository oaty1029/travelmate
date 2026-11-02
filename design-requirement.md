# 旅游项目第二阶段 · 设计需求文档

> **【已过期】本文档描述的是「第二阶段」需求（2026-09-12）。项目已演进至「AI 行程重做」阶段，当前架构与数据库设计以 `docs/DESIGN.md` 为准，本文档仅作历史参考。**

> **版本：2026-09-12 调整版**。预订、订单、支付、退款整套功能与智能推荐**已全部移出本阶段范围，不再开发**。

## 1. 文档概述、修改背景

### 1.1 文档概述

本文档定义旅游项目（TravelMate）第二阶段的需求与设计。最终保留范围：

- **现有功能优化**：移除计划实体状态字段（改为日期计算出行状态）、路线生成算法修复（三类方案差异化 + 费用动态化）。
- **基础数据扩充**：城市 / 景点 / 酒店数据扩充（已完成）。
- **景点评论**：用户发布景点评论、评论刷新通知。
- **端到端回归**：后端编译 + 前端打包 + 回归测试与测试报告。

> 本文档只做需求与设计定义，不包含代码实现；SQL 片段仅用于描述数据库变更。

### 1.2 修改背景

- 技术栈：后端 Spring Boot 3.2.5 + MyBatis-Plus 3.5.5 + MySQL 5.7 + Redis + JWT + PDFBox；前端 Vue 3 + Vite + Element Plus + Pinia。
- 上一阶段（M1–M9）已完成前后端契约修复、计划/论坛/管理后台/安全等模块，系统主流程可用。
- 现状痛点与调整：
  1. 计划原先带有 `status` 状态字段，但业务上**计划只承载行程内容，不需要状态机**；是否出行应由日期客观判断。本阶段移除该字段。
  2. 路线生成的三条方案（时间最优 / 费用最优 / 体验最优）内容几乎一致——天数分配、景点编排、费用估算三处没有按方案类型区分。
  3. 费用估算存在**硬编码虚拟金额**（住宿 150/350/800 元/晚、餐饮 60/150/400 元/天等），应改为从数据库景点 `ticket_price`、酒店 `price_per_night` 动态计算。
  4. 基础数据覆盖不足，且无酒店住宿数据（本阶段已扩充）。
  5. 缺少景点评论能力，用户无法对景点发表评论。

### 1.3 设计原则

1. **增量开发**：优先新增表/字段/接口，尽量不动已调试好的代码。
2. **旧功能零回归**：PDF 导出、论坛评论/发帖/点赞/通知、景点管理等原有能力必须保持可用。
3. **计划无状态**：计划只存行程内容；出行状态由日期计算，不落库、无状态流转接口。
4. **费用动态化**：费用一律从数据库（景点门票单价、酒店每晚单价、城际交通票价）动态计算，禁止硬编码虚拟金额。
5. **防误操作**：评论删除等关键操作二次确认。
6. **前后端契约一致**：新接口沿用现有 `Result<T>` / `PageResult<T>` 统一响应格式。

---

## 2. 用户角色说明

| 角色 | 说明 | 本阶段涉及能力 |
|---|---|---|
| 游客（未登录） | 浏览目的地、城市、景点、酒店 | 浏览扩充后的城市/景点/酒店数据 |
| 普通用户（登录） | 创建计划、生成路线、发布景点评论 | 计划、路线、景点评论 |
| 管理员（admin / super_admin） | 后台管理城市、景点、帖子、用户 | 本次不改动，仅在数据扩充时同步生效 |

> 酒店住宿数据作为「基础数据」扩充，公开可浏览；酒店后台 CRUD 不在本阶段范围。

---

## 3. 功能清单与状态

### 3.1 现有功能优化

| 编号 | 功能 | 优化内容 | 状态 |
|---|---|---|---|
| OPT-1 | 计划实体 | 移除 `status` 状态字段，计划只存行程基础信息 | ✅ 已完成 |
| OPT-2 | 出行状态展示 | 前端列表卡片根据出发/结束日期与当前时间自动计算标签 | ✅ 已完成 |
| OPT-3 | 计划卡片快捷操作 | 卡片外部快捷按钮「编辑、生成方案」 | ✅ 已完成 |
| OPT-4 | 路线生成算法 | 三条方案产出不同景点、不同预算、不同耗时 | ✅ 已完成 |
| OPT-5 | 费用动态化 | 费用从数据库景点/酒店/交通单价动态计算，去硬编码 | ⏳ 待开发 |

### 3.2 全新业务

| 编号 | 功能 | 说明 | 状态 |
|---|---|---|---|
| NEW-1 | 基础数据扩充 | 城市 28→50、景点 72→220+、新增酒店表与数据 | ✅ 已完成 |
| NEW-2 | 景点评论 | 用户发布景点评论、评论刷新通知 | ⏳ 待开发 |
| NEW-3 | 端到端回归 | 后端编译 + 前端打包 + 回归测试与测试报告 | ⏳ 待开发 |

> 已移除：智能推荐、一键预订、订单、支付、退款——以上均不再开发。

---

## 4. 出行状态判定规则（无状态机）

计划实体**不再有状态字段**。是否出行，只依据计划内的 `start_date`（出发日期）、`end_date`（结束日期）与当前时间对比：

| 判定条件 | 出行状态 | 前端标签 | 标签色 |
|---|---|---|---|
| 当前时间 < 出发日期 | 待出行 | 待出行 | primary |
| 出发日期 ≤ 当前时间 ≤ 结束日期 | 正在出行 | 正在出行 | success |
| 当前时间 > 结束日期 | 行程已结束 | 已结束 | info |

补充规则：

1. 未填写出发/结束日期时，默认按「待出行」展示。
2. 计划可**随时编辑、随时重新生成路线方案**，不受任何状态限制。
3. 该出行状态为**前端计算展示**，不落库、不产生状态流转接口。

---

## 5. 数据库变更清单（附 SQL 片段）

> 目标库：MySQL 5.7，字符集 utf8mb4。所有新增表使用 InnoDB。

### 5.1 修改现有表（已完成）

**（1）`user_plans` 移除状态字段**

```sql
ALTER TABLE user_plans DROP COLUMN status;
```

**（2）`route_plans` 新增「预计总耗时」列**

```sql
ALTER TABLE route_plans
  ADD COLUMN total_duration INT NOT NULL DEFAULT 0
  COMMENT '预计总耗时(分钟)' AFTER total_distance;
```

### 5.2 新增表

**（3）酒店表 `hotels`（已完成）**

```sql
CREATE TABLE IF NOT EXISTS hotels (
  id              BIGINT       NOT NULL AUTO_INCREMENT,
  city_id         BIGINT       NOT NULL,
  name            VARCHAR(100) NOT NULL,
  star_level      INT          NOT NULL DEFAULT 3        COMMENT '星级 1-5',
  price_per_night DECIMAL(10,2) NOT NULL                 COMMENT '每晚价格(元)',
  address         VARCHAR(255),
  tags            VARCHAR(255)                            COMMENT 'JSON 标签数组',
  image_url       VARCHAR(500),
  rating          DECIMAL(3,2) NOT NULL DEFAULT 4.0,
  status          INT          NOT NULL DEFAULT 1,
  created_at      DATETIME,
  updated_at      DATETIME,
  PRIMARY KEY (id),
  KEY idx_hotels_city (city_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**（4）景点评论表 `attraction_comments`（待开发）**

```sql
CREATE TABLE IF NOT EXISTS attraction_comments (
  id            BIGINT        NOT NULL AUTO_INCREMENT,
  attraction_id BIGINT        NOT NULL,
  user_id       BIGINT        NOT NULL,
  content       VARCHAR(1000) NOT NULL,
  rating        DECIMAL(2,1)  NULL                      COMMENT '用户打分 1-5，可选',
  status        INT           NOT NULL DEFAULT 1        COMMENT '1 正常 0 隐藏',
  created_at    DATETIME,
  PRIMARY KEY (id),
  KEY idx_ac_attraction (attraction_id),
  KEY idx_ac_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 5.3 基础数据扩充（已完成）

- **城市**：现 28 个 → 扩充至约 50 个。
- **景点**：现约 72 条 → 扩充至 220+ 条，并为每条景点写入差异化的 `rating`、`ticket_price`、`suggest_duration`。
- **酒店**：为主要城市写入经济 / 舒适 / 豪华三档酒店（约 100 家）。
- 交付脚本：`sql/03-hotels-table.sql`（建表）、`sql/04-expand-data.sql`（增量数据）。

---

## 6. 后端接口列表

统一前缀 `/api/v1`；响应结构沿用 `Result<T>`、分页沿用 `PageResult<T>`；除标注「公开」外均需登录。

### 6.1 计划接口（修改，已完成）

| 方法 | 路径 | 说明 | 入参 | 出参 |
|---|---|---|---|---|
| PUT | `/plans/{planId}` | 更新计划（不再包含状态字段） | `PlanUpdateDTO` | `UserPlan` |
| POST | `/plans/{planId}/generate-route` | 生成路线方案（可随时重新生成） | `RouteGenerateDTO` | `List<RoutePlan>` |

> 原有 `POST /plans`、`GET /plans`、`GET /plans/{id}`、`DELETE /plans/{id}`、`/plans/{id}/routes`、`/cost`、`/guide`、`/export`、`/set-recommend`、`/share` 保持不变；已移除原状态相关 `PUT /plans/{planId}/status`。

### 6.2 酒店公开查询接口（已完成）

| 方法 | 路径 | 说明 | 入参 | 出参 |
|---|---|---|---|---|
| GET | `/destination/hotels`（公开） | 某城市酒店分页 | `cityId`, `page`, `pageSize` | `PageResult<Hotel>` |

### 6.3 景点评论接口（待开发）

| 方法 | 路径 | 说明 | 入参 | 出参 |
|---|---|---|---|---|
| GET | `/destination/attractions/{attractionId}/comments`（公开） | 景点评论分页 | `page`, `pageSize` | `PageResult<AttractionComment>` |
| POST | `/destination/attractions/{attractionId}/comments` | 发布评论（登录） | `{ content, rating? }` | `AttractionComment` |
| DELETE | `/destination/attractions/{attractionId}/comments/{commentId}` | 删除自己的评论（登录，二次确认） | — | `void` |

> 评论发布成功后：前端评论列表即时刷新；同时复用现有 `Notification` 通知机制（类型 `comment`）产生通知，通知铃铛未读数 +1。

### 6.4 内部算法改造（费用动态化，待开发）

| 类 | 改造 |
|---|---|
| `AccommodationEstimator` | 住宿费用改为按方案内每个城市停留晚数 × 该城市匹配预算档的酒店 `price_per_night`（从 `hotels` 表动态取），替代 `BudgetConfig` 硬编码 150/350/800 |
| `TicketEstimator` | 门票费用 = 方案内景点 `ticket_price` 求和（保留已有 DB 取值，去掉虚拟乘法系数） |
| `TransportEstimator` | 城际交通从 `transport_cache` 的 `price_low`/`price_high` 动态取，去掉兜底硬编码金额 |
| `BudgetConfig` | 仅保留预算档映射与档位调整逻辑，不再存放金额常量；无 DB 数据源的餐饮/市内交通改为透明估算规则并标注「估算值」 |

---

## 7. 前端页面与组件变更清单、UI 交互要点

### 7.1 修改页面（已完成）

| 页面 | 变更 |
|---|---|
| `MyPlans.vue` | 卡片头部改为**日期计算的出行状态标签**；卡片外部快捷操作「编辑、生成方案」 |
| `PlanEdit.vue` | 不再包含状态表单项 |
| `PlanDetail.vue` | 头部状态标签改为日期计算的出行状态标签 |
| `RouteCompare.vue` | 展示「预计耗时」字段 |

### 7.2 新增页面 / 区块（待开发）

| 页面 / 区块 | 说明 |
|---|---|
| 景点详情评论区 | 在景点浏览/详情处新增评论列表 + 发布框 + 删除（二次确认） |

### 7.3 新增/调整 API 封装

| 文件 | 变更 |
|---|---|
| `api/destination.js` | 新增 `getAttractionComments` / `addAttractionComment` / `deleteAttractionComment`；已有 `getHotels` |
| `api/forum.js` / 通知 | 评论通知复用现有通知接口，无需新增 |

### 7.4 UI 交互要点

1. **视觉统一**：新增按钮、评论框、弹窗沿用 Element Plus 现有风格。
2. **出行状态标签**：纯前端按日期计算，不调用后端状态接口。
3. **评论发布**：发布后列表即时刷新；空内容不可提交；未登录提示登录。
4. **评论删除二次确认**：仅作者本人可删，删除前 `el-popconfirm` 确认。
5. **加载/空态/错误态**：沿用 `v-loading`、`el-empty`、拦截器统一错误提示。

---

## 8. 核心业务流程

### 8.1 路线生成（修复后 + 费用动态化）

三类方案各自独立走完整管线，`costType` 全程透传：

| 方案 | 城市顺序 | 景点选择 | 费用口径 | 耗时口径 |
|---|---|---|---|---|
| 时间最优 `time` | 以城际最短耗时排序 | 短耗时景点优先 | 城际取最快，住宿/餐饮按原档 | 城际耗时 + 景点耗时，最短 |
| 费用最优 `money` | 以最低票价排序 | 免费/低价景点优先 | 城际取最低价，住宿取低价档酒店，门票低价优先 | 相对长 |
| 体验最优 `experience` | 综合排序 | 高评分景点优先 | 城际/住宿按原档或升档，门票全含 | 居中偏长 |

**费用动态化硬性要求**：

- 门票费用 = 方案内景点 `ticket_price` 求和（数据库值）。
- 住宿费用 = Σ(每个城市停留晚数 × 该城市匹配预算档的酒店 `price_per_night`)。
- 城际交通 = 依据 `transport_cache` 实际票价。
- **禁止任何硬编码虚拟金额**；无 DB 数据源的杂项（餐饮/市内交通）只能用透明规则估算并明确标注。

### 8.2 景点评论

1. 用户在景点详情/评论区块输入评论内容（可附带 1–5 评分）。
2. 提交后写入 `attraction_comments`，前端评论列表即时刷新。
3. 复用现有通知机制产生 `comment` 通知，通知铃铛未读数 +1。
4. 作者可删除自己的评论（二次确认）。

---

## 9. 测试要点与验收标准

### 9.1 出行状态计算（日期标签）

- 出发日期在未来 → 「待出行」；出发日期 ≤ 今天 ≤ 结束日期 → 「正在出行」；结束日期已过 → 「已结束」；未填日期 → 「待出行」。
- 编辑计划、重新生成路线方案不受出行状态限制。

### 9.2 三种路线方案差异化 + 费用动态化

- 同一计划三条方案，景点列表 / 总费用 / 预计耗时至少一项明显不同（目标：三项均不同）。
- 费用最优总费用 ≤ 时间最优 ≤ 体验最优（典型场景）；时间最优耗时最短。
- 断言费用全部来自数据库单价（无硬编码常量），住宿费用与 `hotels.price_per_night` 一致。

### 9.3 景点评论

- 登录用户可发布评论，列表即时刷新；未登录被拦截。
- 空内容/超长内容被校验拒绝。
- 作者可删除自己的评论（二次确认）；他人不可删。
- 评论产生通知，通知铃铛未读数 +1。

### 9.4 回归测试（原有功能不破坏）

- PDF 导出正常（中文、¥ 符号不崩）。
- 论坛评论 / 发帖 / 点赞 / 通知正常。
- 城市 / 景点管理（后台 CRUD）正常。
- 计划创建 / 编辑 / 删除 / 分享 / 收藏正常。

### 9.5 验收标准

- 后端 `mvn clean compile` 通过。
- 前端 `vite build` 通过。
- 端到端回归测试全部通过，无旧功能回归，测试报告交付。

---

## 附：已确认决策（2026-09-12 更新）

1. **移除计划状态字段**：计划不再有状态机；出行状态由日期计算展示。
2. **预订/订单/支付/退款整套功能不再开发**，从范围中全部移除。
3. **智能推荐不再开发**，从范围中移除。
4. **费用动态化**：费用从数据库景点/酒店/交通单价动态计算，禁止硬编码虚拟金额。
5. **酒店后台管理**：不纳入，仅公开查询 + 数据扩充。
6. **数据写入方式**：SQL 脚本（SeedRunner 仅在全新环境幂等初始化时同步扩充）。
7. **保留待办**：路线费用动态化、景点评论（发布 + 刷新通知）、端到端回归测试与报告——等用户指令分批执行。
