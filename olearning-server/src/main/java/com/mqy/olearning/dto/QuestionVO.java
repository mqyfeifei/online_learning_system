package com.mqy.olearning.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 题目返回对象。学生答题时不返回 answer / analysis。
 * <p>NON_NULL 很关键：Jackson 默认会把 null 字段也序列化成 {@code "answer":null}，
 * 虽然值不泄露，但响应体里出现 answer 字样容易让人误判为泄题，也给了前端误用的机会。
 * 加上 NON_NULL 后未赋值的字段直接消失。
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QuestionVO {

    private Long id;
    private Integer type;
    private String title;
    private List<Map<String, String>> options;
    private Integer score;

    /**
     * 试卷题目关联表的主键，只有「教师看试卷题目」时才有值。
     * 移除题目要按这个 id 删 paper_question，不能用题干 id（id 是 question.id）。
     */
    private Long paperQuestionId;

    private String answer;
    /** 问答题参考答案（仅教师视角返回） */
    private String referenceAnswer;
    private String analysis;
}
