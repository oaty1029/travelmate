package com.travelmate.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.Map;

/**
 * 保存 AI 行程请求体
 */
@Data
public class ItinerarySaveDTO {

    /** 标题（选填，缺省取 result.summary.title） */
    private String title;

    /** 输入快照（13 数据项） */
    @Valid
    @NotNull(message = "输入信息必填")
    private AiGenerateDTO input;

    /** AI 返回的行程 JSON */
    @NotNull(message = "行程结果必填")
    private Map<String, Object> result;
}
