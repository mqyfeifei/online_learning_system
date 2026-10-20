package com.mqy.olearning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 讨论帖 */
@Data
@TableName("discussion_post")
public class DiscussionPost {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long courseId;
    private Long userId;
    private String title;
    private String content;
    private Integer likeCount;
    /** 1 正常 0 已删除（管理员审核） */
    private Integer status;
    private LocalDateTime createTime;
}
