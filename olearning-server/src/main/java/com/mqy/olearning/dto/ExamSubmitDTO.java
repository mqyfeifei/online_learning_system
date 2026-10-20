package com.mqy.olearning.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

/** 交卷入参：answers 的 key 是题目 ID，value 是学生答案 */
@Data
public class ExamSubmitDTO {

    @NotNull(message = "试卷 ID 不能为空")
    private Long paperId;

    private Map<String, String> answers;
}
