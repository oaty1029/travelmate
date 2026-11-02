# sql/ 目录说明

本目录包含数据库脚本。目标库：`travelmate`（MySQL 5.7）。

## 全新环境入口

`00-init-schema.sql` —— 14 张在用表的全量结构（含索引 / 外键，与线上库一致）。

```bash
mysql --default-character-set=utf8mb4 -uroot -p < 00-init-schema.sql
```

务必带 `--default-character-set=utf8mb4`，否则中文注释会乱码。

## 历史迁移脚本（01–13）

按编号记录项目从「第二阶段」到「AI 行程重做」的演进过程，是**针对既有库的增量迁移**，全新库无需按序执行（先跑 `00-init-schema.sql`，再启动后端由 SeedRunner 写入基础数据）。

| 文件 | 内容 | 状态 |
| ---- | ---- | ---- |
| 01-plan-status-migration.sql | 删除 `user_plans.status` 列 | 旧表，已归档 |
| 02-route-costtype-migration.sql | `route_plans` 新增 `total_duration` | 旧表，已归档 |
| 03-hotels-table.sql | 新建 `hotels` 酒店表 | 已归档 `*_bak` |
| 04-expand-data.sql | 数据扩充（城市 28→50、景点 72→220+、酒店） | 旧表，已归档 |
| 05-attraction-comments.sql | 新建 `attraction_comments` 景点评论表 | 已归档 `*_bak` |
| 06-ai-itinerary.sql | 新建 `itineraries` + `ai_config`；`forum_posts` 加 `itinerary_id` | 当前在用 |
| 07-archive-old-tables.sql | 旧行程/攻略/景点/酒店等表统一改名 `*_bak` | 归档动作 |
| 08-attractions-recreate.sql | 重建 `attractions`（新增 `address`/`image_urls`）并从 `attractions_bak` 恢复 | 当前在用 |
| 09-city-hot-migration.sql | `cities` 新增 `is_popular`（热门城市轮播） | 当前在用 |
| 10-city-enrichment.sql | 城市扩充（66 城介绍 / 特色美食 / 乡土人情 / 封面） | 当前在用 |
| 11-attraction-enrichment.sql | 景点扩充（248 景点介绍 / 分类 / 多图 + 新增） | 当前在用 |
| 12-forum-comments-upgrade.sql | 论坛子评论 + 评论点赞/不感兴趣 + 去重浏览 | 当前在用 |
| 13-notification-itinerary.sql | `notifications` 新增 `itinerary_id` | 当前在用 |

## 注意事项

- 迁移脚本依赖既有库状态（如 08 从 `attractions_bak` 恢复、10 更新既有城市、11 更新既有景点），不是全新库的完整数据路径。
- 城市 / 景点完整数据可由 `scripts/` 下的生成脚本重新生成。
- 执行迁移后需重启后端，接口才会读到新表 / 新列。
