package com.mqy.olearning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 课程表 */
@Data
@TableName("course")
public class Course {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long teacherId;
    private Long categoryId;
    private String title;
    private String cover;
    private String description;
    /** 0 草稿 1 待审核 2 已发布 3 已下架 4 审核驳回 */
    private Integer status;
    /** 驳回理由 */
    private String rejectReason;
    private Integer viewCount;
    private LocalDateTime createTime;
}
