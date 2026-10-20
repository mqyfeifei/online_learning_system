package com.mqy.olearning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 系统公告 */
@Data
@TableName("sys_notice")
public class SysNotice {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String content;
    /** 1 置顶（列名 top_flag，避开保留字 top） */
    private Integer topFlag;
    /** 1 显示 0 撤下 */
    private Integer status;
    private LocalDateTime createTime;
}
