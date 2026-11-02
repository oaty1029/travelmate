-- =============================================================
-- 批8：城市+景点信息库 - 重建 attractions 表并从备份恢复旧数据
-- 目标库：travelmate (MySQL 5.7)
-- 说明：
--   1) 此前 07-archive-old-tables.sql 已将 attractions 改名为 attractions_bak。
--   2) 本脚本重建 attractions：保留旧字段 + 新增 address(地址)、image_urls(多图 JSON 数组)。
--   3) 从 attractions_bak 恢复全部旧景点（id 原样保留，address/image_urls 置空）。
--   4) 幂等：仅当 attractions 为空时执行恢复，避免重复插入。
--   5) 不 DROP attractions_bak（保留备份）。
-- =============================================================

USE travelmate;

CREATE TABLE IF NOT EXISTS attractions (
  id               BIGINT        NOT NULL AUTO_INCREMENT,
  city_id          BIGINT        NOT NULL,
  name             VARCHAR(191)  NOT NULL,
  category         VARCHAR(50)   NOT NULL COMMENT '分类：自然景观/历史遗迹/博物馆/宗教/主题公园/街区',
  description      TEXT,
  rating           DECIMAL(3,1)  NOT NULL DEFAULT 4.0 COMMENT '评分 0-5',
  ticket_price     DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '门票(元)',
  suggest_duration INT           NOT NULL DEFAULT 120 COMMENT '建议游玩时长(分钟)',
  open_time        VARCHAR(100),
  best_season      VARCHAR(100),
  address          VARCHAR(255),
  image_urls       TEXT COMMENT '多图 JSON 数组，如 ["/images/x.jpg","/images/y.jpg"]',
  status           INT           NOT NULL DEFAULT 1 COMMENT '0=下架 1=上架',
  created_at       DATETIME,
  updated_at       DATETIME,
  PRIMARY KEY (id),
  KEY idx_attractions_city (city_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 从备份恢复旧数据（仅当新表为空）
INSERT INTO attractions
  (id, city_id, name, category, description, rating, ticket_price,
   suggest_duration, open_time, best_season, status, created_at, updated_at)
SELECT
  id, city_id, name, category, description, rating, ticket_price,
  suggest_duration, open_time, best_season, status, created_at, NOW()
FROM attractions_bak
WHERE NOT EXISTS (SELECT 1 FROM attractions);
