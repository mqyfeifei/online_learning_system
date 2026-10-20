package com.mqy.olearning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 用户表 */
@Data
@TableName("sys_user")
public class SysUser {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    /** 存储的是 MD5(明文 + salt)，不存明文 */
    private String password;
    private String salt;
    private String realName;
    private String avatar;
    /** STUDENT / TEACHER / ADMIN */
    private String role;
    private String phone;
    /** 1 启用 0 禁用 */
    private Integer status;
    private LocalDateTime createTime;
}
