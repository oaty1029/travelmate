# TravelMate 旅伴 · 系统设计文档

> 当前版本（AI 行程重做阶段，2026-10）。替代已过期的 `design-requirement.md`。

## 1. 项目概述

TravelMate 是一个旅游行程规划 + 社区分享平台：

- 用户浏览目的地（省份 → 城市 → 景点），输入出行偏好后用 DeepSeek AI 一键生成多日行程；
- 用户在论坛发布行程攻略、评论、点赞；
- 管理员通过独立后台管理用户、审核内容、维护目的地与 AI 配置。

## 2. 总体架构

前后端分离，双前端（用户端 + 管理端），后端单体提供 REST API，数据层 MySQL 5.7 + Redis 缓存，AI 能力接入 DeepSeek。

```
浏览器（用户端 frontend :5173）      ─┐
浏览器（管理端 admin-frontend :5174） ─┼─→ 后端 Spring Boot :3000 ─┬─→ MySQL 5.7
                                      │                            └─→ Redis 7
                                      └─→ DeepSeek API（AI 行程生成）
```

## 3. 技术栈

| 层 | 技术 |
| ---- | ---- |
| 后端 | Spring Boot 3.2.5（Java 17）· MyBatis-Plus 3.5.5 · MySQL 5.7 · Redis 7 · JWT（jjwt 0.12.5）· Knife4j 4.5.0 · PDFBox 3.0.2 · BCrypt |
| 用户端 | Vue 3.4 · Vite 5 · Element Plus 2.7 · Pinia · Vue Router · Axios · ECharts 5 · dayjs · DOMPurify |
| 管理端 | 同上（独立工程，ECharts 数据看板） |
| AI | DeepSeek（chat/completions，管理后台动态配置） |

## 4. 后端模块结构

```
com.travelmate
├── common/       统一返回 Result/PageResult、分页工具、业务异常、全局异常处理
├── config/       Web 配置（CORS/拦截器/静态资源）、MyBatis-Plus、Redis、BCrypt、
│                 SeedRunner（种子数据）、Swagger/Knife4j
├── controller/   10 个 REST 控制器
├── service/      6 个业务服务（Admin/Ai/Destination/Forum/Itinerary/User）
├── mapper/       14 个 MyBatis-Plus Mapper（对应 14 张表）
├── entity/       14 个实体
├── dto/          12 个请求/响应 DTO
└── security/     JwtUtil + JwtAuthInterceptor（拦截器鉴权）
```

控制器：

| 控制器 | 职责 |
| ---- | ---- |
| AuthController | 注册 / 登录 |
| UserController | 个人信息、修改密码 |
| DestinationController | 省份 / 城市 / 景点浏览、热门推荐 |
| AiController | AI 行程生成 |
| ItineraryController | 行程保存 / 列表 / 详情 / 收藏 |
| ForumController | 帖子 / 评论 / 点赞 / 浏览 |
| AdminController | 用户管理、内容审核、目的地管理、AI 配置、操作日志、数据看板 |
| ImageController | 图片上传 |
| CityImageController | 城市图片 |
| HealthController | 健康检查 |

## 5. 数据库设计

14 张在用表（结构见 `sql/00-init-schema.sql`，与线上库一致）：

| 表 | 说明 |
| ---- | ---- |
| provinces | 省份（34） |
| cities | 城市（66，含热门标记 / 经纬度 / 特色美食 / 乡土人情） |
| attractions | 景点（248，多图 JSON / 门票 / 评分 / 建议时长） |
| users | 用户（手机 / 邮箱唯一，BCrypt 密码） |
| forum_categories | 论坛分类 |
| forum_posts | 帖子（可挂载行程、审核隐藏） |
| forum_comments | 评论（支持子评论） |
| forum_likes | 帖子点赞（去重） |
| forum_post_views | 帖子去重浏览 |
| forum_comment_reactions | 评论点赞 / 不感兴趣 |
| notifications | 通知（点赞 / 评论 / 回复 / 行程推荐） |
| itineraries | AI 行程（输入快照 + AI 返回 JSON） |
| ai_config | AI 平台动态配置（provider/base_url/api_key/model） |
| operation_logs | 管理操作日志 |

主要外键关系：

- `cities.province_id → provinces.id`
- `forum_posts.user_id → users.id`、`forum_posts.category_id → forum_categories.id`
- `forum_comments.post_id → forum_posts.id`（级联删除）、`forum_comments.user_id → users.id`
- `forum_likes.post_id → forum_posts.id`（级联删除）、`forum_likes.user_id → users.id`
- `notifications.user_id / post_id / from_user_id → users.id / forum_posts.id`
- `attractions`、`itineraries` 仅建索引，无外键约束

## 6. 核心业务流程

### 6.1 认证

- 注册：BCrypt 加密密码入库；登录返回 JWT（有效期 7 天）。
- 鉴权：`JwtAuthInterceptor` 拦截 `/api/**`，校验 `Authorization: Bearer <token>`；白名单放行登录 / 注册 / 公开浏览接口。
- 用户身份经 ThreadLocal / 请求属性在本次请求内传递。

### 6.2 AI 行程生成

1. 用户提交出发地 / 目的地 / 日期 / 天数 / 人数 / 人均预算 / 酒店偏好等 13 项输入（8 必填 + 5 选填）。
2. 后端从 `ai_config` 读取启用的 DeepSeek 配置，构造 system prompt（固定 JSON 格式：summary / days / costBreakdown / tips）。
3. 调用 DeepSeek `chat/completions`，解析返回 JSON，校验：days 非空、价格非负、分项合计 == total == 总预算、总预算落在「人均预算 × 人数」的 0.3–3 倍合理带内。
4. 失败重试 1 次；最终结果存 `itineraries.result_json`，输入快照存 `input_json`。

### 6.3 论坛与通知

- 发帖可挂载已生成行程；评论支持回复子评论（parent_id / reply_to_user_id）。
- 点赞 / 浏览均按「用户 + 帖子」去重。
- 点赞 / 评论 / 回复 / 行程推荐触发通知。

### 6.4 管理审核

- 帖子隐藏（记录 hidden_by / hidden_reason）、批量隐藏 / 删除。
- 用户禁用、城市景点上下架、AI 配置维护，均写入 `operation_logs`。

## 7. API 规范

- 前缀：`/api/v1`（40+ 接口）。
- 统一响应：`Result<T>`（`code / message / data`）、分页 `PageResult<T>`。
- 鉴权头：`Authorization: Bearer <token>`。
- 接口文档：http://localhost:3000/doc.html（Knife4j）。

## 8. 前端结构

- `frontend/`（用户端，14 视图）：首页热门城市轮播、目的地浏览、AI 行程生成、行程管理、论坛、通知、个人中心。
- `admin-frontend/`（管理端，9 视图）：数据看板（ECharts）、用户管理、帖子审核、目的地管理、AI 配置、操作日志。
- 开发时 Vite 代理 `/api`、`/images` 到后端 3000 端口。

## 9. 部署

- 本地开发：MySQL 5.7 + Redis 本机运行，后端 IntelliJ 启动（3000），两个前端 `npm run dev`（5173 / 5174）。
- Docker：`docker-compose up` 启动 MySQL 5.7 + Redis + 后端 + 用户端前端（管理端暂未纳入）。
