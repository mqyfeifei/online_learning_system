package com.mqy.olearning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 作业表 */
@Data
@TableName("assignment")
public class Assignment {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long courseId;
    private String title;
    /** 作业要求（列名 requirement，避开 SQL 保留字 require） */
    private String requirement;
    private String attachUrl;
    /** 开放作答时间；为空表示不限开始时间 */
    private LocalDateTime startTime;
    private LocalDateTime deadline;
    private LocalDateTime createTime;
}
