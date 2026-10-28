package com.mqy.olearning.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginVO {

    private String token;
    private Long id;
    /** 学号 / 职工号 */
    private String userNo;
    private String username;
    private String realName;
    private String role;
    private String avatar;
}
