package com.travelmate.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 景点表（城市附属景点信息库）
 */
@Data
@TableName("attractions")
public class Attraction {
    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("city_id")
    private Long cityId;
    private String name;
    private String category;
    private String description;
    private BigDecimal rating;
    @TableField("ticket_price")
    private BigDecimal ticketPrice;
    @TableField("suggest_duration")
    private Integer suggestDuration;
    @TableField("open_time")
    private String openTime;
    @TableField("best_season")
    private String bestSeason;
    private String address;
    /** 多图 JSON 数组字符串，如 ["/images/x.jpg","/images/y.jpg"] */
    @TableField("image_urls")
    private String imageUrls;
    private Integer status = 1;
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
