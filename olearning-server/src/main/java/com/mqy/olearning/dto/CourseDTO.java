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
    /** 选课是否需要审核：1 需要 0 不需要 */
    private Integer enrollAudit;
    /** 课件资料是否允许下载：1 允许 0 仅在线查看 */
    private Integer resourceDownloadable;
    /** 资料导出范围：NONE / ENROLLED / ALL */
    private String exportScope;
}
