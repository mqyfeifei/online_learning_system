package com.mqy.olearning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 试卷 */
@Data
@TableName("exam_paper")
public class ExamPaper {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long courseId;
    private String title;
    private Integer totalScore;
    private Integer passScore;
    /** 考试时长（分钟） */
    private Integer duration;
    /** 1 启用 0 停用 */
    private Integer status;
    private LocalDateTime createTime;
}
