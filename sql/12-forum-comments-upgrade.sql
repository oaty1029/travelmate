-- =============================================================
-- 批12：论坛评论升级（子评论 + 点赞/不感兴趣 + 去重浏览）
-- 目标库：travelmate (MySQL 5.7)
-- 说明：1) forum_comments 增加 parent_id/reply_to_user_id/like_count/not_interested_count；
--       2) 新表 forum_post_views 记录去重浏览，forum_comment_reactions 记录评论点赞/不感兴趣；
--       3) 旧 view_count 为点击次数（无法还原为去重用户数），重置为 0 重新累计。
-- =============================================================

USE travelmate;

-- 1. 评论表加列（子评论 + 点赞/不感兴趣）
ALTER TABLE forum_comments
  ADD COLUMN parent_id            BIGINT NULL COMMENT '顶层评论的父评论 id，顶层为 NULL' AFTER user_id,
  ADD COLUMN reply_to_user_id     BIGINT NULL COMMENT '被回复的具体用户 id（回复子评论时指向该评论作者）' AFTER parent_id,
  ADD COLUMN like_count           INT    NOT NULL DEFAULT 0 COMMENT '点赞数' AFTER content,
  ADD COLUMN not_interested_count INT    NOT NULL DEFAULT 0 COMMENT '不感兴趣数' AFTER like_count,
  ADD KEY idx_comments_parent (parent_id);

-- 2. 帖子去重浏览表（同一用户对同一帖子只计一次）
CREATE TABLE IF NOT EXISTS forum_post_views (
  id         BIGINT NOT NULL AUTO_INCREMENT,
  post_id    BIGINT NOT NULL,
  user_id    BIGINT NOT NULL,
  created_at DATETIME,
  PRIMARY KEY (id),
  UNIQUE KEY uk_post_user (post_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. 评论反应表（点赞 / 不感兴趣）
CREATE TABLE IF NOT EXISTS forum_comment_reactions (
  id         BIGINT      NOT NULL AUTO_INCREMENT,
  user_id    BIGINT      NOT NULL,
  comment_id BIGINT      NOT NULL,
  type       VARCHAR(20) NOT NULL COMMENT 'like / not_interested',
  created_at DATETIME,
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_comment_type (user_id, comment_id, type),
  KEY idx_reaction_comment (comment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4. 旧 view_count 为点击次数，无法还原去重人数，重置为 0
UPDATE forum_posts SET view_count = 0;
