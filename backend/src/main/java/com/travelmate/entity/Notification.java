package com.travelmate.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 通知表
 */
@Data
@TableName("notifications")
public class Notification {
    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("user_id")
    private Long userId;
    /** like / comment / reply / system */
    private String type;
    private String title;
    private String content;
    @TableField("post_id")
    private Long postId;
    @TableField("itinerary_id")
    private Long itineraryId;
    @TableField("from_user_id")
    private Long fromUserId;
    @TableField("is_read")
    private Integer isRead = 0;
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
