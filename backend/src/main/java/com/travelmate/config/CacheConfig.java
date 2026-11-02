package com.travelmate.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * 启用 Spring Cache（默认用 ConcurrentMapCache，有 Redis 时自动用 Redis）
 */
@Configuration
@EnableCaching
public class CacheConfig {
}
