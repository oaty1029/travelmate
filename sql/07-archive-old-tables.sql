-- =============================================================
-- 批7：旧表备份改名（不 DROP，保留数据）
-- 目标库：travelmate (MySQL 5.7)
-- 说明：将旧的行程/攻略/景点/酒店/交通/分享/收藏相关表统一改名 *_bak。
--       幂等：已存在 *_bak 或原表不存在时跳过。
-- =============================================================

USE travelmate;

DROP PROCEDURE IF EXISTS rename_if_exists;
DELIMITER $$
CREATE PROCEDURE rename_if_exists(IN old_name VARCHAR(64), IN new_name VARCHAR(64))
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.tables
    WHERE table_schema = DATABASE() AND table_name = old_name
  ) AND NOT EXISTS (
    SELECT 1 FROM information_schema.tables
    WHERE table_schema = DATABASE() AND table_name = new_name
  ) THEN
    SET @s = CONCAT('RENAME TABLE `', old_name, '` TO `', new_name, '`');
    PREPARE stmt FROM @s;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  END IF;
END$$
DELIMITER ;

CALL rename_if_exists('user_plans',         'user_plans_bak');
CALL rename_if_exists('plan_cities',        'plan_cities_bak');
CALL rename_if_exists('route_plans',        'route_plans_bak');
CALL rename_if_exists('share_links',        'share_links_bak');
CALL rename_if_exists('attractions',        'attractions_bak');
CALL rename_if_exists('attraction_comments','attraction_comments_bak');
CALL rename_if_exists('hotels',             'hotels_bak');
CALL rename_if_exists('transport_cache',    'transport_cache_bak');
CALL rename_if_exists('favorites',          'favorites_bak');

DROP PROCEDURE rename_if_exists;
