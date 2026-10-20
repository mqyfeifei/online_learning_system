package com.mqy.olearning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 作业提交记录 */
@Data
@TableName("submission")
public class Submission {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long assignmentId;
    private Long userId;
    private String content;
    private String attachUrl;
    private Integer score;
    private String comment;
    /** 0 已提交待批阅 1 已批阅 */
    private Integer status;
    private LocalDateTime submitTime;
}
