package com.travelmate.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 省份表（34 条）
 */
@Data
@TableName("provinces")
public class Province {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    /** 华北/东北/华东/中南/西南/西北/港澳台 */
    private String region;
    private String pinyin;
    @TableField("sort_order")
    private Integer sortOrder = 0;
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
