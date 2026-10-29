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
    /** 客观题自动判分得分 */
    private Integer objectiveScore;
    /** 主观题教师给分 */
    private Integer subjectiveScore;
    /** 0 无需批阅 1 待批阅 2 已批阅 */
    private Integer markStatus;

    /** 错题与解析明细 */
    private List<AnswerDetail> details;

    @Data
    public static class AnswerDetail {
        private Long questionId;
        /** 1 单选 2 多选 3 判断 4 填空 5 问答 */
        private Integer type;
        private String title;
        /** 选择题的选项 [{key:"A", text:"..."}]，学生看解析时需要看到选项内容 */
        private java.util.List<java.util.Map<String, String>> options;
        private String yourAnswer;
        private String rightAnswer;
        /** 问答题的参考答案，学生答卷详情里也会展示，便于对照 */
        private String referenceAnswer;
        private Boolean correct;
        private Integer score;
        private Integer fullScore;
        private String analysis;
        /** 需要教师人工批阅（问答题） */
        private Boolean needManual;
        /** 教师评语 */
        private String comment;
    }
}
