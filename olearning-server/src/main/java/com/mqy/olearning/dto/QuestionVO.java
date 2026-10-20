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

    private String answer;
    private String analysis;
}
