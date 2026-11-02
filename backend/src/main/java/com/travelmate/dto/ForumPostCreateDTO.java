package com.travelmate.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ForumPostCreateDTO {
    @NotNull(message = "分类不能为空")
    private Long categoryId;
    @NotBlank(message = "标题不能为空")
    @Size(max = 100, message = "标题最长 100 字")
    private String title;
    @NotBlank(message = "内容不能为空")
    @Size(max = 5000, message = "内容最长 5000 字")
    private String content;
    @Size(max = 191, message = "封面链接过长")
    private String coverUrl;
    /** 挂载的用户自己的行程 ID（可选） */
    private Long itineraryId;
}
