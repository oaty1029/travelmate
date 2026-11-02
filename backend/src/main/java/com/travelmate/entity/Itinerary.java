package com.travelmate.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * AI 行程表（替代 user_plans）
 */
@Data
@TableName("itineraries")
public class Itinerary {
    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("user_id")
    private Long userId;
    private String title;
    /** 13 字段输入快照 JSON */
    @TableField("input_json")
    private String inputJson;
    /** AI 返回行程 JSON（固定格式） */
    @TableField("result_json")
    private String resultJson;
    @TableField("total_days")
    private Integer totalDays;
    @TableField("total_budget")
    private BigDecimal totalBudget;
    /** 收藏来源行程 ID，NULL=原创 */
    @TableField("copy_from_id")
    private Long copyFromId;
    /** 0=否 1=管理员推荐首页 */
    @TableField("is_recommended")
    private Integer isRecommended;
    @TableField("recommend_reason")
    private String recommendReason;
    /** 作者昵称（非表字段，管理后台列表填充） */
    @TableField(exist = false)
    private String authorName;
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
