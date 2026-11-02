package com.travelmate.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 论坛帖子表
 */
@Data
@TableName("forum_posts")
public class ForumPost {
    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("category_id")
    private Long categoryId;
    @TableField("user_id")
    private Long userId;
    private String title;
    private String content;
    /** 挂载的行程 ID（forum_posts.itinerary_id） */
    @TableField("itinerary_id")
    private Long itineraryId;
    /** 封面图 URL（DB 暂无此列，暂标记非持久化） */
    @TableField(exist = false)
    private String coverUrl;
    @TableField("view_count")
    private Integer viewCount = 0;
    @TableField("like_count")
    private Integer likeCount = 0;
    @TableField("comment_count")
    private Integer commentCount = 0;
    /** 1=已发布 / 0=草稿 / 2=隐藏（匹配 DB int 类型） */
    private Integer status = 1;
    @TableField("hidden_at")
    private LocalDateTime hiddenAt;
    @TableField("hidden_by")
    private Long hiddenBy;
    @TableField("hidden_by_role")
    private String hiddenByRole;
    @TableField("hide_reason")
    private String hideReason;
    /** 展示字段（非持久化，由服务层填充） */
    @TableField(exist = false)
    private String authorName;
    @TableField(exist = false)
    private String authorAvatar;
    @TableField(exist = false)
    private String categoryName;
    /** 当前登录用户是否已点赞（非表字段，详情接口按需填充） */
    @TableField(exist = false)
    private Boolean liked;
    /** 挂载行程内容 result_json（非表字段，详情接口按需填充） */
    @TableField(exist = false)
    private Map<String, Object> itinerary;
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
