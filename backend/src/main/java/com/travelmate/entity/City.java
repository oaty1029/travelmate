package com.travelmate.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 城市表（50+ 条）
 */
@Data
@TableName("cities")
public class City {
    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("province_id")
    private Long provinceId;
    /** 冗余返回字段（非表列），由服务层填充 */
    @TableField(exist = false)
    private String provinceName;
    private String name;
    private String pinyin;
    private String description;
    /** 特色美食 */
    @TableField("specialty_food")
    private String specialtyFood;
    /** 乡土人情 */
    @TableField("local_culture")
    private String localCulture;
    @TableField("image_url")
    private String imageUrl;
    @TableField("best_season")
    private String bestSeason;
    /** JSON 字符串存储标签数组 */
    private String tags;
    private Integer popularity = 0;
    /** 0=否 1=管理员设为热门（主页轮播优先） */
    @TableField("is_popular")
    private Integer isPopular = 0;
    private BigDecimal lat;
    private BigDecimal lng;
    private Integer status = 1;
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
