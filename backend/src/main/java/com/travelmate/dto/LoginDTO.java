package com.travelmate.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LoginDTO {
    /** 可以是 email / phone / nickname */
    @NotBlank(message = "账号不能为空")
    @Size(max = 100, message = "账号最长100个字符")
    private String account;

    @NotBlank(message = "密码不能为空")
    @Size(max = 64, message = "密码最长64位")
    private String password;
}
