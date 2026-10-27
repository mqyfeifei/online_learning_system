package com.mqy.olearning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 主观题批阅明细：一题一行，记录教师给分与评语 */
@Data
@TableName("exam_mark")
public class ExamMark {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long recordId;
    private Long questionId;
    private Integer score;
    private String comment;
    private Long teacherId;
    private LocalDateTime updateTime;
}
