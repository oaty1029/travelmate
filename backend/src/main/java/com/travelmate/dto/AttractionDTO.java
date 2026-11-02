package com.travelmate.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

/**
 * 景点创建/更新 DTO（管理员）
 */
@Data
public class AttractionDTO {
    @NotNull(message = "所属城市不能为空")
    private Long cityId;

    @NotBlank(message = "景点名不能为空")
    @Size(max = 100, message = "景点名最长 100 字")
    private String name;

    @NotBlank(message = "分类不能为空")
    @Size(max = 50, message = "分类最长 50 字")
    private String category;

    @Size(max = 2000, message = "描述最长 2000 字")
    private String description;

    @DecimalMin(value = "0", message = "评分不能为负")
    @DecimalMax(value = "5", message = "评分最大 5")
    private BigDecimal rating;

    @DecimalMin(value = "0", message = "门票不能为负")
    private BigDecimal ticketPrice;

    @Min(value = 1, message = "建议时长至少 1 小时")
    @Max(value = 24, message = "建议时长最大 24 小时")
    private Integer suggestDuration;

    @Size(max = 100, message = "开放时间最长 100 字")
    private String openTime;

    @Size(max = 100, message = "最佳季节最长 100 字")
    private String bestSeason;

    @Size(max = 255, message = "地址最长 255 字")
    private String address;

    /** 多图 JSON 数组字符串，如 ["/images/x.jpg","/images/y.jpg"]，可为空 */
    @Size(max = 2000, message = "图片列表过长")
    private String imageUrls;

    /** 0=下架 1=上架 */
    private Integer status;
}
