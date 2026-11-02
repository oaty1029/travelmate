package com.travelmate.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

/**
 * 城市管理创建/更新 DTO（管理员）
 */
@Data
public class CityDTO {
    @NotBlank(message = "城市名不能为空")
    @Size(max = 191, message = "城市名最长 191 字")
    private String name;

    @Size(max = 191, message = "拼音最长 191 字")
    private String pinyin;

    @NotNull(message = "省份不能为空")
    private Long provinceId;

    @Size(max = 2000, message = "描述最长 2000 字")
    private String description;

    @Size(max = 2000, message = "特色美食最长 2000 字")
    private String specialtyFood;

    @Size(max = 2000, message = "乡土人情最长 2000 字")
    private String localCulture;

    @Size(max = 191, message = "图片 URL 最长 191 字")
    private String imageUrl;

    @Size(max = 191, message = "最佳季节最长 191 字")
    private String bestSeason;

    /** JSON 数组字符串，如 ["历史","文化"]，可为空 */
    @Size(max = 500, message = "标签最长 500 字")
    private String tags;

    @Min(value = 0, message = "热度不能为负")
    @Max(value = 9999, message = "热度最大 9999")
    private Integer popularity;

    /** 0=否 1=管理员设为热门 */
    private Integer isPopular;

    @DecimalMin(value = "-90", message = "纬度范围 -90~90")
    @DecimalMax(value = "90", message = "纬度范围 -90~90")
    private BigDecimal lat;

    @DecimalMin(value = "-180", message = "经度范围 -180~180")
    @DecimalMax(value = "180", message = "经度范围 -180~180")
    private BigDecimal lng;

    /** 0=下架 1=上架 */
    private Integer status;
}
