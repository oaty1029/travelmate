-- =============================================================
-- 批6：AI 行程重做 - 新表 + 论坛附行程列
-- 目标库：travelmate (MySQL 5.7)
-- 说明：行程生成改为 DeepSeek AI 生成，新表 itineraries 存输入快照与 AI 返回 JSON；
--       ai_config 存管理后台动态配置的 AI 平台信息；
--       论坛帖子 forum_posts 增加 itinerary_id 关联附带行程。
--       旧行程相关表在"删旧代码"批次中统一改名 *_bak 备份，本文件不处理。
-- =============================================================

USE travelmate;

-- 行程主表（替代 user_plans）
CREATE TABLE IF NOT EXISTS itineraries (
  id               BIGINT        NOT NULL AUTO_INCREMENT,
  user_id          BIGINT        NOT NULL,
  title            VARCHAR(200)  NOT NULL,
  input_json       TEXT          NOT NULL COMMENT '13字段输入快照 JSON',
  result_json      TEXT          NOT NULL COMMENT 'AI 返回行程 JSON（固定格式）',
  total_days       INT           NOT NULL DEFAULT 0,
  total_budget     DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '总预算(元)',
  copy_from_id     BIGINT        NULL COMMENT '收藏来源行程ID，NULL=原创',
  is_recommended   INT           NOT NULL DEFAULT 0 COMMENT '0=否 1=管理员推荐首页',
  recommend_reason VARCHAR(255)  NULL COMMENT '推荐语',
  created_at       DATETIME,
  updated_at       DATETIME,
  PRIMARY KEY (id),
  KEY idx_itineraries_user (user_id),
  KEY idx_itineraries_recommended (is_recommended)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- AI 平台配置（管理后台动态维护，单行）
CREATE TABLE IF NOT EXISTS ai_config (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  provider    VARCHAR(50)  NOT NULL DEFAULT 'deepseek',
  base_url    VARCHAR(255) NOT NULL,
  api_key     VARCHAR(255) NOT NULL,
  model       VARCHAR(100) NOT NULL,
  temperature DECIMAL(3,2) NOT NULL DEFAULT 1.0,
  max_tokens  INT          NOT NULL DEFAULT 4096,
  enabled     INT          NOT NULL DEFAULT 1 COMMENT '0=停用 1=启用',
  updated_at  DATETIME,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO ai_config (provider, base_url, api_key, model, temperature, max_tokens, enabled, updated_at)
VALUES ('deepseek', 'https://api.deepseek.com', '', 'deepseek-chat', 1.0, 4096, 1, NOW());

-- 论坛帖子附带行程
ALTER TABLE forum_posts ADD COLUMN itinerary_id BIGINT NULL COMMENT '附带行程ID' AFTER content;
