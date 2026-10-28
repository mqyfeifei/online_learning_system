package com.mqy.olearning.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginDTO {

    /** 登录账号 = 学号 / 职工号 */
    @NotBlank(message = "请输入学号或职工号")
    private String account;

    @NotBlank(message = "密码不能为空")
    private String password;
}
