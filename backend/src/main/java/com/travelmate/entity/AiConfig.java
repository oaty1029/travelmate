package com.travelmate.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * AI 平台配置表（管理后台动态维护，单行）
 */
@Data
@TableName("ai_config")
public class AiConfig {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String provider;
    @TableField("base_url")
    private String baseUrl;
    @TableField("api_key")
    private String apiKey;
    private String model;
    private BigDecimal temperature;
    @TableField("max_tokens")
    private Integer maxTokens;
    private Integer enabled;
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
