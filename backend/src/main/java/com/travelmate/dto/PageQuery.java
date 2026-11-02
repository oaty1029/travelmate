package com.travelmate.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 分页查询基类 — 所有列表接口统一接收 page/pageSize 参数
 */
@Data
public class PageQuery {

    @Min(value = 1, message = "page 最小为 1")
    private int page = 1;

    @Min(value = 1, message = "pageSize 最小为 1")
    @Max(value = 100, message = "pageSize 最大为 100")
    private int pageSize = 10;
}
