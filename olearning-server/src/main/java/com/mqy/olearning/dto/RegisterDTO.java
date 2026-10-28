package com.mqy.olearning.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterDTO {

    /** 学号 / 职工号：登录用，全平台唯一 */
    @NotBlank(message = "学号/职工号不能为空")
    @Pattern(regexp = "^[A-Za-z0-9_-]{4,32}$", message = "学号/职工号为 4-32 位字母、数字、- 或 _")
    private String userNo;

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度 3-20 位")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度 6-20 位")
    private String password;

    private String realName;
    private String phone;

    /** STUDENT / TEACHER，注册接口不允许传 ADMIN */
    private String role = "STUDENT";
}
