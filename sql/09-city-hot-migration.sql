-- =============================================================
-- 批9：城市表新增「是否热门」字段（管理员设置主页轮播热门城市）
-- 目标库：travelmate (MySQL 5.7)
-- 说明：执行一次即可；已存在该列时需手动跳过。
-- =============================================================

USE travelmate;

ALTER TABLE cities
  ADD COLUMN is_popular INT NOT NULL DEFAULT 0
  COMMENT '0=否 1=管理员设为热门(主页轮播优先)' AFTER popularity;
