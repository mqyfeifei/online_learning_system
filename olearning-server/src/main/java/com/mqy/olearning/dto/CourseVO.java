package com.mqy.olearning.dto;

import com.mqy.olearning.entity.Course;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 课程列表/详情返回对象，在实体基础上补展示字段 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CourseVO extends Course {

    private String teacherName;
    private String categoryName;
    private Integer chapterCount;
    private Integer studentCount;
    /** 当前用户是否已选 */
    private Boolean enrolled;
    /** 当前用户的学习进度百分比 0-100 */
    private Integer progress;
}
