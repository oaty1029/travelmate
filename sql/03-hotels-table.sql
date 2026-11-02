-- =============================================================
-- 批3：酒店表建表
-- 目标库：travelmate (MySQL 5.7)
-- 说明：酒店作为基础数据扩充，公开可浏览；酒店后台 CRUD 不在本阶段范围。
-- =============================================================

USE travelmate;

CREATE TABLE IF NOT EXISTS hotels (
  id              BIGINT        NOT NULL AUTO_INCREMENT,
  city_id         BIGINT        NOT NULL,
  name            VARCHAR(100)  NOT NULL,
  star_level      INT           NOT NULL DEFAULT 3        COMMENT '星级 1-5',
  price_per_night DECIMAL(10,2) NOT NULL                  COMMENT '每晚价格(元)',
  address         VARCHAR(255),
  tags            VARCHAR(255)                            COMMENT 'JSON 标签数组',
  image_url       VARCHAR(500),
  rating          DECIMAL(3,2)  NOT NULL DEFAULT 4.0,
  status          INT           NOT NULL DEFAULT 1,
  created_at      DATETIME,
  updated_at      DATETIME,
  PRIMARY KEY (id),
  KEY idx_hotels_city (city_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

