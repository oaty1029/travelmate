package com.travelmate.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.List;

/**
 * 帖子批量操作 DTO（管理员）
 */
@Data
public class PostBatchDTO {
    @NotEmpty(message = "帖子 ID 列表不能为空")
    @Size(max = 100, message = "单次最多操作 100 条")
    private List<Long> ids;

    @Size(max = 191, message = "原因最长 191 字")
    private String reason;
}
