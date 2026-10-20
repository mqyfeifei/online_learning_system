package com.mqy.olearning.dto;

import lombok.Data;

import java.util.List;

@Data
public class ExamResultVO {

    private Long recordId;
    private Integer score;
    private Integer totalScore;
    private Integer passScore;
    private Boolean passed;

    /** 错题与解析明细 */
    private List<AnswerDetail> details;

    @Data
    public static class AnswerDetail {
        private Long questionId;
        private String title;
        private String yourAnswer;
        private String rightAnswer;
        private Boolean correct;
        private Integer score;
        private String analysis;
    }
}
