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
    /** 总分 = 客观题自动分 + 主观题教师分 */
    private Integer score;
    /** 客观题自动判分得分 */
    private Integer objectiveScore;
    /** 主观题教师给分 */
    private Integer subjectiveScore;
    /** 0 无需批阅 1 待批阅 2 已批阅 */
    private Integer markStatus;
    private LocalDateTime markTime;
    private Long markerId;
    /** 1 及格 0 不及格（列名 passed，避开关键字 pass） */
    private Integer passed;
    private LocalDateTime startTime;
    private LocalDateTime submitTime;
}
