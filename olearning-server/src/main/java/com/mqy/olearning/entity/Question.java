package com.mqy.olearning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 题库 */
@Data
@TableName("question")
public class Question {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long courseId;
    /** 1 单选 2 多选 3 判断 */
    private Integer type;
    private String title;
    /** 选项 JSON，例：[{"key":"A","text":"..."},...]，判断题可为空 */
    private String optionsJson;
    /** 正确答案，多选用逗号分隔，如 "A,C"；判断题为 "T"/"F" */
    private String answer;
    private String analysis;
    /** 1 简单 2 中等 3 困难 */
    private Integer difficulty;
    private LocalDateTime createTime;
}
