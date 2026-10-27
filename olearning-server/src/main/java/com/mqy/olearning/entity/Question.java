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
    /** 1 单选 2 多选 3 判断 4 填空 5 问答 */
    private Integer type;
    private String title;
    /** 选项 JSON，例：[{"key":"A","text":"..."},...]，判断题/填空/问答为空 */
    private String optionsJson;
    /**
     * 标准答案：多选逗号分隔（A,C）；判断题 T/F；
     * 填空题多空用 | 分隔、同空多种正确写法用 ~ 分隔；问答题为空
     */
    private String answer;
    /** 问答题参考答案（无标准答案，教师批阅时参考） */
    private String referenceAnswer;
    private String analysis;
    /** 1 简单 2 中等 3 困难 */
    private Integer difficulty;
    private LocalDateTime createTime;
}
