package com.mqy.olearning.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 教师端课程新增/编辑入参 */
@Data
public class CourseDTO {

    private Long id;

    @NotBlank(message = "课程标题不能为空")
    private String title;

    private Long categoryId;
    private String cover;
    private String description;
}
