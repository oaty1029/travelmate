package com.travelmate.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 资料编辑入参 — 字段均可选，仅校验格式；服务层决定更新哪些字段
 */
@Data
public class UpdateProfileDTO {
    @Size(min = 2, max = 30, message = "昵称长度须在2-30个字符之间")
    private String nickname;

    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Size(max = 191, message = "头像地址过长")
    private String avatarUrl;
}
