-- =============================================================
-- TravelMate 旅伴 · 数据库初始化脚本（全量结构）
-- 目标库：MySQL 5.7（实际运行环境 5.7.30-log）
-- 说明：14 张在用表的结构，按线上实际库导出（含索引与外键），
--       不含 *_bak 归档表与 _prisma_migrations。
--       索引/外键/唯一键名与线上库完全一致；中文注释按源码/迁移
--       脚本还原（线上库部分早期列注释因建表时连接字符集错误已
--       乱码，此处已修正）。
-- 用法（务必带 --default-character-set=utf8mb4，否则中文注释乱码）：
--   mysql --default-character-set=utf8mb4 -uroot -p < 00-init-schema.sql
-- =============================================================

CREATE DATABASE IF NOT EXISTS travelmate DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE travelmate;

SET FOREIGN_KEY_CHECKS = 0;

-- ---------- 省份表 ----------
DROP TABLE IF EXISTS `provinces`;
CREATE TABLE `provinces` (
  `id`         int(11)      NOT NULL AUTO_INCREMENT,
  `name`       varchar(191) NOT NULL,
  `region`     varchar(191) NOT NULL COMMENT '华北/东北/华东/中南/西南/西北/港澳台',
  `pinyin`     varchar(191) DEFAULT NULL,
  `sort_order` int(11)      NOT NULL DEFAULT '0',
  `created_at` datetime(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------- 用户表 ----------
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users` (
  `id`            int(11)      NOT NULL AUTO_INCREMENT,
  `phone`         varchar(191) DEFAULT NULL,
  `email`         varchar(191) DEFAULT NULL,
  `password_hash` varchar(191) NOT NULL,
  `nickname`      varchar(191) DEFAULT NULL,
  `avatar_url`    varchar(191) DEFAULT NULL,
  `role`          varchar(191) NOT NULL DEFAULT 'user' COMMENT 'user / admin',
  `status`        int(11)      NOT NULL DEFAULT '1' COMMENT '1=正常 0=禁用',
  `created_at`    datetime(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at`    datetime(3)  NOT NULL,
  PRIMARY KEY (`id`),
  -- 线上库存在 4 个唯一键（users_phone_key/users_email_key 与 uk_phone/uk_email 重复），此处如实保留
  UNIQUE KEY `users_phone_key` (`phone`),
  UNIQUE KEY `users_email_key` (`email`),
  UNIQUE KEY `uk_email` (`email`),
  UNIQUE KEY `uk_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------- 论坛分类表 ----------
DROP TABLE IF EXISTS `forum_categories`;
CREATE TABLE `forum_categories` (
  `id`          int(11)      NOT NULL AUTO_INCREMENT,
  `name`        varchar(191) NOT NULL,
  `description` text,
  `sort_order`  int(11)      NOT NULL DEFAULT '0',
  `status`      int(11)      NOT NULL DEFAULT '1',
  `created_at`  datetime(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `forum_categories_name_key` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------- 城市表 ----------
DROP TABLE IF EXISTS `cities`;
CREATE TABLE `cities` (
  `id`             int(11)      NOT NULL AUTO_INCREMENT,
  `province_id`    int(11)      NOT NULL,
  `name`           varchar(191) NOT NULL,
  `pinyin`         varchar(191) DEFAULT NULL,
  `description`    text,
  `specialty_food` text COMMENT '特色美食',
  `local_culture`  text COMMENT '乡土人情',
  `image_url`      varchar(191) DEFAULT NULL,
  `best_season`    varchar(191) DEFAULT NULL,
  `tags`           json         DEFAULT NULL,
  `popularity`     int(11)      NOT NULL DEFAULT '0',
  `is_popular`     int(11)      NOT NULL DEFAULT '0' COMMENT '0=否 1=管理员设为热门(主页轮播优先)',
  `lat`            decimal(10,7) DEFAULT NULL,
  `lng`            decimal(10,7) DEFAULT NULL,
  `status`         int(11)      NOT NULL DEFAULT '1',
  `created_at`     datetime(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at`     datetime(3)  NOT NULL,
  PRIMARY KEY (`id`),
  KEY `cities_province_id_status_idx` (`province_id`,`status`),
  CONSTRAINT `cities_province_id_fkey` FOREIGN KEY (`province_id`) REFERENCES `provinces` (`id`) ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------- 论坛帖子表 ----------
DROP TABLE IF EXISTS `forum_posts`;
CREATE TABLE `forum_posts` (
  `id`             int(11)      NOT NULL AUTO_INCREMENT,
  `user_id`        int(11)      NOT NULL,
  `category_id`    int(11)      NOT NULL,
  `title`          varchar(191) NOT NULL,
  `content`        text         NOT NULL,
  `itinerary_id`   bigint(20)   DEFAULT NULL COMMENT '附带行程ID',
  `status`         int(11)      NOT NULL DEFAULT '1' COMMENT '1=已发布 0=草稿 2=隐藏',
  `view_count`     int(11)      NOT NULL DEFAULT '0',
  `like_count`     int(11)      NOT NULL DEFAULT '0',
  `comment_count`  int(11)      NOT NULL DEFAULT '0',
  `created_at`     datetime(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at`     datetime(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `hidden_at`      datetime(3)  DEFAULT NULL,
  `hidden_by`      int(11)      DEFAULT NULL,
  `hidden_by_role` varchar(191) DEFAULT NULL,
  `hide_reason`    text,
  PRIMARY KEY (`id`),
  KEY `forum_posts_user_id_idx` (`user_id`),
  KEY `forum_posts_category_id_status_idx` (`category_id`,`status`),
  CONSTRAINT `forum_posts_category_id_fkey` FOREIGN KEY (`category_id`) REFERENCES `forum_categories` (`id`) ON UPDATE CASCADE,
  CONSTRAINT `forum_posts_user_id_fkey` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------- 论坛评论表（支持子评论） ----------
DROP TABLE IF EXISTS `forum_comments`;
CREATE TABLE `forum_comments` (
  `id`                   int(11)     NOT NULL AUTO_INCREMENT,
  `post_id`              int(11)     NOT NULL,
  `user_id`              int(11)     NOT NULL,
  `parent_id`            bigint(20)  DEFAULT NULL COMMENT '顶层评论的父评论 id，顶层为 NULL',
  `reply_to_user_id`     bigint(20)  DEFAULT NULL COMMENT '被回复的具体用户 id（回复子评论时指向该评论作者）',
  `content`              text        NOT NULL,
  `like_count`           int(11)     NOT NULL DEFAULT '0' COMMENT '点赞数',
  `not_interested_count` int(11)     NOT NULL DEFAULT '0' COMMENT '不感兴趣数',
  `status`               int(11)     NOT NULL DEFAULT '1',
  `created_at`           datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `forum_comments_user_id_fkey` (`user_id`),
  KEY `forum_comments_post_id_status_idx` (`post_id`,`status`),
  KEY `idx_comments_parent` (`parent_id`),
  CONSTRAINT `forum_comments_post_id_fkey` FOREIGN KEY (`post_id`) REFERENCES `forum_posts` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `forum_comments_user_id_fkey` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------- 论坛点赞表 ----------
DROP TABLE IF EXISTS `forum_likes`;
CREATE TABLE `forum_likes` (
  `id`         int(11)     NOT NULL AUTO_INCREMENT,
  `post_id`    int(11)     NOT NULL,
  `user_id`    int(11)     NOT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `forum_likes_post_id_user_id_key` (`post_id`,`user_id`),
  KEY `forum_likes_user_id_fkey` (`user_id`),
  CONSTRAINT `forum_likes_post_id_fkey` FOREIGN KEY (`post_id`) REFERENCES `forum_posts` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `forum_likes_user_id_fkey` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------- 通知表 ----------
DROP TABLE IF EXISTS `notifications`;
CREATE TABLE `notifications` (
  `id`           int(11)      NOT NULL AUTO_INCREMENT,
  `user_id`      int(11)      NOT NULL,
  `type`         varchar(191) NOT NULL COMMENT 'like / comment / reply / system',
  `title`        varchar(191) NOT NULL,
  `content`      varchar(191) NOT NULL,
  `post_id`      int(11)      DEFAULT NULL,
  `itinerary_id` bigint(20)   DEFAULT NULL COMMENT '关联的行程 id（行程推荐通知）',
  `from_user_id` int(11)      DEFAULT NULL,
  `is_read`      tinyint(1)   NOT NULL DEFAULT '0',
  `created_at`   datetime(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `notifications_user_id_is_read_idx` (`user_id`,`is_read`),
  KEY `notifications_user_id_created_at_idx` (`user_id`,`created_at`),
  KEY `notifications_post_id_fkey` (`post_id`),
  KEY `notifications_from_user_id_fkey` (`from_user_id`),
  KEY `idx_notification_itinerary` (`itinerary_id`),
  CONSTRAINT `notifications_from_user_id_fkey` FOREIGN KEY (`from_user_id`) REFERENCES `users` (`id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `notifications_post_id_fkey` FOREIGN KEY (`post_id`) REFERENCES `forum_posts` (`id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `notifications_user_id_fkey` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------- 帖子浏览去重表 ----------
DROP TABLE IF EXISTS `forum_post_views`;
CREATE TABLE `forum_post_views` (
  `id`         bigint(20) NOT NULL AUTO_INCREMENT,
  `post_id`    bigint(20) NOT NULL,
  `user_id`    bigint(20) NOT NULL,
  `created_at` datetime   DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_post_user` (`post_id`,`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------- 论坛评论反应表 ----------
DROP TABLE IF EXISTS `forum_comment_reactions`;
CREATE TABLE `forum_comment_reactions` (
  `id`         bigint(20)  NOT NULL AUTO_INCREMENT,
  `user_id`    bigint(20)  NOT NULL,
  `comment_id` bigint(20)  NOT NULL,
  `type`       varchar(20) NOT NULL COMMENT 'like / not_interested',
  `created_at` datetime    DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_comment_type` (`user_id`,`comment_id`,`type`),
  KEY `idx_reaction_comment` (`comment_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------- 景点表 ----------
DROP TABLE IF EXISTS `attractions`;
CREATE TABLE `attractions` (
  `id`               bigint(20)    NOT NULL AUTO_INCREMENT,
  `city_id`          bigint(20)    NOT NULL,
  `name`             varchar(191)  NOT NULL,
  `category`         varchar(50)   NOT NULL COMMENT '分类：自然景观/历史遗迹/博物馆/宗教/主题公园/街区',
  `description`      text,
  `rating`           decimal(3,1)  NOT NULL DEFAULT '4.0' COMMENT '评分 0-5',
  `ticket_price`     decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '门票(元)',
  `suggest_duration` int(11)       NOT NULL DEFAULT '120' COMMENT '建议游玩时长(分钟)',
  `open_time`        varchar(100)  DEFAULT NULL,
  `best_season`      varchar(100)  DEFAULT NULL,
  `address`          varchar(255)  DEFAULT NULL,
  `image_urls`       text COMMENT '多图 JSON 数组，如 ["/images/x.jpg","/images/y.jpg"]',
  `status`           int(11)       NOT NULL DEFAULT '1' COMMENT '0=下架 1=上架',
  `created_at`       datetime      DEFAULT NULL,
  `updated_at`       datetime      DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_attractions_city` (`city_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------- AI 行程表 ----------
DROP TABLE IF EXISTS `itineraries`;
CREATE TABLE `itineraries` (
  `id`               bigint(20)    NOT NULL AUTO_INCREMENT,
  `user_id`          bigint(20)    NOT NULL,
  `title`            varchar(200)  NOT NULL,
  `input_json`       text          NOT NULL COMMENT '13字段输入快照 JSON',
  `result_json`      text          NOT NULL COMMENT 'AI 返回行程 JSON（固定格式）',
  `total_days`       int(11)       NOT NULL DEFAULT '0',
  `total_budget`     decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '总预算(元)',
  `copy_from_id`     bigint(20)    DEFAULT NULL COMMENT '收藏来源行程ID，NULL=原创',
  `is_recommended`   int(11)       NOT NULL DEFAULT '0' COMMENT '0=否 1=管理员推荐首页',
  `recommend_reason` varchar(255)  DEFAULT NULL COMMENT '推荐语',
  `created_at`       datetime      DEFAULT NULL,
  `updated_at`       datetime      DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_itineraries_user` (`user_id`),
  KEY `idx_itineraries_recommended` (`is_recommended`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------- 操作日志表 ----------
DROP TABLE IF EXISTS `operation_logs`;
CREATE TABLE `operation_logs` (
  `id`            int(11)      NOT NULL AUTO_INCREMENT,
  `operator_id`   int(11)      NOT NULL,
  `operator_role` varchar(191) NOT NULL,
  `action`        varchar(191) NOT NULL COMMENT 'create / update / delete / status / batch_hide / batch_delete',
  `target_type`   varchar(191) NOT NULL,
  `target_id`     int(11)      NOT NULL,
  `reason`        text,
  `created_at`    datetime(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `operation_logs_target_type_target_id_idx` (`target_type`,`target_id`),
  KEY `operation_logs_operator_id_idx` (`operator_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------- AI 平台配置表 ----------
DROP TABLE IF EXISTS `ai_config`;
CREATE TABLE `ai_config` (
  `id`          bigint(20)   NOT NULL AUTO_INCREMENT,
  `provider`    varchar(50)  NOT NULL DEFAULT 'deepseek',
  `base_url`    varchar(255) NOT NULL,
  `api_key`     varchar(255) NOT NULL,
  `model`       varchar(100) NOT NULL,
  `temperature` decimal(3,2) NOT NULL DEFAULT '1.00',
  `max_tokens`  int(11)      NOT NULL DEFAULT '4096',
  `enabled`     int(11)      NOT NULL DEFAULT '1' COMMENT '0=停用 1=启用',
  `updated_at`  datetime     DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET FOREIGN_KEY_CHECKS = 1;
