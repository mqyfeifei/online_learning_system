package com.mqy.olearning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 学习笔记：可以挂在课程、章节上，也可以挂在某次作业 / 某张试卷上 */
@Data
@TableName("study_note")
public class StudyNote {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long courseId;
    private Long chapterId;
    /** 关联对象类型：ASSIGNMENT / EXAM；为空表示普通笔记 */
    private String refType;
    /** 关联对象 id */
    private Long refId;
    private String title;
    private String content;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
