-- =============================================================
-- 批2：路线生成算法修复 - route_plans 新增「预计总耗时」列
-- 目标库：travelmate (MySQL 5.7)
-- 说明：时间最优/费用最优/体验最优 三种方案需产出不同耗时，
--       total_duration 承载「城际交通耗时 + 景点游玩耗时」的差异。
-- =============================================================

USE travelmate;

ALTER TABLE route_plans
  ADD COLUMN total_duration INT NOT NULL DEFAULT 0
  COMMENT '预计总耗时(分钟)' AFTER total_distance;

