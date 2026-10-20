package com.mqy.olearning.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginVO {

    private String token;
    private Long id;
    private String username;
    private String realName;
    private String role;
    private String avatar;
}
