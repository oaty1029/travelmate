-- =============================================================
-- 批4：景点评论表（与论坛评论 forum_comments 独立）
-- 目标库：travelmate (MySQL 5.7)
-- =============================================================

USE travelmate;

CREATE TABLE IF NOT EXISTS attraction_comments (
  id            BIGINT         NOT NULL AUTO_INCREMENT,
  attraction_id BIGINT         NOT NULL,
  user_id       BIGINT         NOT NULL,
  parent_id     BIGINT         NULL                        COMMENT '回复的父评论 id，顶层评论为 NULL',
  content       VARCHAR(1000)  NOT NULL,
  rating        DECIMAL(2,1)   NULL                        COMMENT '用户打分 1-5，可选',
  status        INT            NOT NULL DEFAULT 1          COMMENT '1 正常 0 隐藏',
  created_at    DATETIME(3)    NOT NULL,
  PRIMARY KEY (id),
  KEY idx_ac_attraction (attraction_id),
  KEY idx_ac_user (user_id),
  KEY idx_ac_parent (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
