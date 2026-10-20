package com.mqy.olearning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 课件资源（PDF / PPT / MP4） */
@Data
@TableName("course_resource")
public class CourseResource {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long courseId;
    private Long chapterId;
    private String name;
    private String fileUrl;
    private String fileType;
    private Long fileSize;
    private LocalDateTime createTime;
}
