package com.mqy.olearning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 考试记录 */
@Data
@TableName("exam_record")
public class ExamRecord {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long paperId;
    private Long userId;
    /** 学生答案 JSON：{"题目ID":"A,C"} */
    private String answersJson;
    private Integer score;
    /** 1 及格 0 不及格（列名 passed，避开关键字 pass） */
    private Integer passed;
    private LocalDateTime startTime;
    private LocalDateTime submitTime;
}
