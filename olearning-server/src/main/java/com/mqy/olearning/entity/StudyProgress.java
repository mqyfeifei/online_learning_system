package com.mqy.olearning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 学习进度：一个用户在一个章节上只留一条记录 */
@Data
@TableName("study_progress")
public class StudyProgress {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long courseId;
    private Long chapterId;
    /** 1 已学完 */
    private Integer finished;
    private LocalDateTime finishTime;
}
