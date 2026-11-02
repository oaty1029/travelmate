# TravelMate 旅伴

旅游行程规划 + 社区分享平台。用户可浏览目的地（省 / 市 / 景点），使用 DeepSeek AI 一键生成多日行程，并在论坛发帖、评论、点赞；管理员通过独立后台管理用户、内容与 AI 配置。

## 技术栈

**后端**：Spring Boot 3.2.5（Java 17）· MyBatis-Plus 3.5.5 · MySQL 5.7 · Redis 7 · JWT（jjwt 0.12.5）· Knife4j 4.5.0 · PDFBox 3.0.2 · BCrypt

**前端**：Vue 3.4 · Vite 5 · Element Plus 2.7 · Pinia · Vue Router · Axios · ECharts 5 · dayjs · DOMPurify

**AI**：DeepSeek（chat/completions，动态配置）

## 目录结构

```
TravelProject/
├── backend/          # Spring Boot 后端（端口 3000）
├── frontend/         # 用户端（Vite dev 端口 5173）
├── admin-frontend/   # 管理端（Vite dev 端口 5174）
├── sql/              # 数据库脚本（00-init-schema.sql + 01-13 历史迁移）
├── scripts/          # 数据生成 / 处理脚本（Python）
└── docker-compose.yml
```

## 功能特性

- 用户认证：注册 / 登录，JWT 无状态鉴权，BCrypt 密码加密
- 目的地浏览：省份 → 城市 → 景点，热门城市轮播、景点多图
- AI 行程生成：输入出发地 / 目的地 / 日期 / 人数 / 预算等，DeepSeek 生成多日行程，含费用明细与预算校验
- 论坛社区：帖子分类、子评论、点赞、浏览去重
- 通知系统：点赞 / 评论 / 回复 / 行程推荐通知
- 管理后台：用户管理、内容审核（隐藏 / 删除）、城市景点管理、AI 配置、操作日志、数据看板

## 环境要求

- JDK 17+（本机实测 JDK 21 编译运行）
- Maven 3.9+
- Node.js 18+ / npm
- MySQL 5.7（线上实际运行库为 5.7.30）
- Redis 7

## 快速开始

### 1. 数据库与 Redis

启动本机 MySQL 5.7 与 Redis，然后建库建表（务必带 `--default-character-set=utf8mb4`，否则中文注释乱码）：

```bash
mysql --default-character-set=utf8mb4 -uroot -p < sql/00-init-schema.sql
```

脚本会创建 `travelmate` 库与 14 张在用表（结构与线上库一致）。

### 2. 后端（端口 3000）

- 方式一（推荐）：IntelliJ IDEA 打开 `backend/`，运行主类。
- 方式二：`cd backend && mvn spring-boot:run`

启动时自动写入省份 / 城市 / 管理员 / 论坛分类基础数据。接口文档：http://localhost:3000/doc.html（Knife4j）。

### 3. 用户端（端口 5173）

```bash
cd frontend
npm install
npm run dev
```

访问 http://localhost:5173

### 4. 管理端（端口 5174）

```bash
cd admin-frontend
npm install
npm run dev
```

访问 http://localhost:5174

### 5. Docker（可选）

```bash
docker-compose up
```

一键启动 MySQL 5.7 + Redis + 后端 + 用户端前端（管理端暂未纳入 compose）。

## 默认账号

| 角色 | 账号 | 密码 |
| ---- | ---- | ---- |
| 管理员 | admin@travelmate.com | Admin@123 |

（由后端启动时种子数据创建，仅用于开发 / 演示。）

## 数据说明

- `sql/00-init-schema.sql`：14 张在用表的全量结构（含索引 / 外键，与线上库一致）。
- `sql/01-13`：历史演进迁移脚本（针对既有库逐步改造，全新库无需按序全跑）。
- 城市 / 景点完整数据：见 `sql/08`、`10`、`11` 及 `scripts/` 生成脚本（这些迁移依赖既有库状态，非全新库完整数据路径）。
- 图片：`backend/uploads/images/`，可由 `scripts/` 重新生成。

## 关键配置

- `backend/src/main/resources/application.yml`：数据库连接、Redis、JWT、上传目录。
- AI 平台（DeepSeek）参数在管理后台「AI 配置」动态维护（表 `ai_config`）。

## 开发注意事项

- 后端从 IntelliJ 启动，改完源码需 Stop → Run 重启才生效。
- SQL 脚本需手动执行，不会随应用自动运行。
- 后端工作目录是项目根目录（非 `backend/`），上传目录已配为 `./backend/uploads`。
