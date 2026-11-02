package com.travelmate.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 论坛评论表（支持子评论：parent_id 指向顶层评论）
 */
@Data
@TableName("forum_comments")
public class ForumComment {
    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("post_id")
    private Long postId;
    @TableField("user_id")
    private Long userId;
    /** 顶层评论的父评论 id，顶层为 NULL */
    @TableField("parent_id")
    private Long parentId;
    /** 被回复的具体用户 id（回复子评论时指向该评论作者） */
    @TableField("reply_to_user_id")
    private Long replyToUserId;
    private String content;
    @TableField("like_count")
    private Integer likeCount = 0;
    @TableField("not_interested_count")
    private Integer notInterestedCount = 0;
    /** 展示字段（非持久化，由服务层填充） */
    @TableField(exist = false)
    private String authorName;
    @TableField(exist = false)
    private String authorAvatar;
    /** 回复中显示的被回复人昵称（非持久化） */
    @TableField(exist = false)
    private String replyToUserName;
    /** 顶层评论下的回复数量（非持久化） */
    @TableField(exist = false)
    private Integer replyCount = 0;
    /** 顶层评论下的回复列表（非持久化） */
    @TableField(exist = false)
    private List<ForumComment> replies;
    /** 当前用户是否已点赞 / 已标记不感兴趣（非持久化） */
    @TableField(exist = false)
    private Boolean liked;
    @TableField(exist = false)
    private Boolean notInterested;
    private Integer status = 1;
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
