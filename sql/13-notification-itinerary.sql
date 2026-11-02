-- =============================================================
-- 批13：通知表增加 itinerary_id（行程被推荐通知）
-- 目标库：travelmate (MySQL 5.7)
-- =============================================================

USE travelmate;

ALTER TABLE notifications
  ADD COLUMN itinerary_id BIGINT NULL COMMENT '关联的行程 id（行程推荐通知）' AFTER post_id,
  ADD KEY idx_notification_itinerary (itinerary_id);
