package com.travelmate.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 论坛评论反应表 - 点赞 / 不感兴趣（type: like / not_interested）
 */
@Data
@TableName("forum_comment_reactions")
public class ForumCommentReaction {
    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("user_id")
    private Long userId;
    @TableField("comment_id")
    private Long commentId;
    /** like / not_interested */
    private String type;
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
