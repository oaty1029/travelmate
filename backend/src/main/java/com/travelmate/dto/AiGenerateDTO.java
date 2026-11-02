package com.travelmate.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * AI 行程生成输入（13 数据项：8 必填 + 5 选填）
 */
@Data
public class AiGenerateDTO {

    /** 出发地 */
    @NotBlank(message = "出发地必填")
    private String departure;

    /** 目的地（多城市） */
    @NotEmpty(message = "目的地必填")
    private List<String> destinations;

    /** 出发日期 */
    @NotNull(message = "出发日期必填")
    private LocalDate startDate;

    /** 游玩天数 */
    @NotNull(message = "游玩天数必填")
    @Min(value = 1, message = "游玩天数至少 1 天")
    @Max(value = 15, message = "游玩天数最多 15 天")
    private Integer days;

    /** 出行人数 */
    @NotNull(message = "出行人数必填")
    @Min(value = 1, message = "出行人数至少 1 人")
    private Integer people;

    /** 人均预算（元） */
    @NotNull(message = "人均预算必填")
    @DecimalMin(value = "0", message = "人均预算不能为负")
    private BigDecimal budgetPerPerson;

    /** 预算档次：经济/舒适/豪华 */
    @NotBlank(message = "预算档次必填")
    private String budgetLevel;

    /** 酒店偏好：经济/舒适/豪华 */
    @NotBlank(message = "酒店偏好必填")
    private String hotelLevel;

    /** 同行人构成（选填）：成人/儿童/老人 */
    private List<String> companions;

    /** 旅行兴趣（选填）：自然风光/历史人文/美食/亲子/购物/休闲 */
    private List<String> interests;

    /** 出行方式（选填）：自驾/高铁/飞机/公共交通 */
    private String transport;

    /** 餐饮偏好（选填） */
    private String diet;

    /** 行程节奏（选填）：轻松/紧凑/均衡 */
    private String pace;
}
