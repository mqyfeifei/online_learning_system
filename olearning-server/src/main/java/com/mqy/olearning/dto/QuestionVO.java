package com.mqy.olearning.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

/** 题目返回对象。学生答题时不返回 answer / analysis */
@Data
public class QuestionVO {

    private Long id;
    private Integer type;
    private String title;
    private List<Map<String, String>> options;
    private Integer score;

    private String answer;
    private String analysis;
}
