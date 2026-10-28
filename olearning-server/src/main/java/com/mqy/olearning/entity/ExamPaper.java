package com.mqy.olearning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
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
    /** 可空：挂到某一章节，空表示整门课 */
    private Long chapterId;
    private String title;
    private Integer totalScore;
    private Integer passScore;
    /** 考试时长（分钟） */
    private Integer duration;
    /** 开放作答时间；为空表示不限开始时间 */
    private LocalDateTime startTime;
    /** 关闭作答时间；为空表示不限结束时间 */
    private LocalDateTime endTime;
    /** 1 启用 0 停用 */
    private Integer status;
    /** 0 未发布 1 已发布；只有已发布的试卷学生才看得到 */
    private Integer publishStatus;
    private LocalDateTime publishedAt;
    /** 发布版本号：每次重新发布 +1，学生答卷记录对应版本 */
    private Integer version;
    private LocalDateTime createTime;

    // ---------------- 展示字段（非表列，教师端列表用） ----------------
    /** 已选题目数 */
    @TableField(exist = false)
    private Integer questionCount;
    /** 已选题目分值合计 */
    @TableField(exist = false)
    private Integer questionScore;
    /** 所属章节标题 */
    @TableField(exist = false)
    private String chapterTitle;
}
