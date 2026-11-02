package com.travelmate.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 操作日志表
 */
@Data
@TableName("operation_logs")
public class OperationLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("operator_id")
    private Long operatorId;
    @TableField("operator_role")
    private String operatorRole;
    /** 动作：create / update / delete / status / batch_hide / batch_delete */
    private String action;
    @TableField("target_type")
    private String targetType;
    @TableField("target_id")
    private Long targetId;
    private String reason;
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
