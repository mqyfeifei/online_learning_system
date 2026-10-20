package com.mqy.olearning.service;

import com.mqy.olearning.mapper.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 学习进度计算单元测试（对应测试用例 TC-05）
 */
@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock private CourseMapper courseMapper;
    @Mock private ChapterMapper chapterMapper;
    @Mock private EnrollmentMapper enrollmentMapper;
    @Mock private StudyProgressMapper progressMapper;
    @Mock private SysUserMapper userMapper;
    @Mock private CourseCategoryMapper categoryMapper;
    @Mock private CourseResourceMapper resourceMapper;

    @InjectMocks
    private CourseService courseService;

    @Test
    @DisplayName("TC-05 学完 3/10 章，进度应为 30%")
    void progressThreeOfTen() {
        when(progressMapper.selectCount(any())).thenReturn(3L);
        assertEquals(30, courseService.calcProgress(4L, 1L, 10));
    }

    @Test
    @DisplayName("全部学完进度为 100%")
    void progressAllFinished() {
        when(progressMapper.selectCount(any())).thenReturn(4L);
        assertEquals(100, courseService.calcProgress(4L, 1L, 4));
    }

    @Test
    @DisplayName("课程没有章节时进度为 0，不能出现除零")
    void progressZeroChapter() {
        assertEquals(0, courseService.calcProgress(4L, 1L, 0));
    }

    @Test
    @DisplayName("一条都没学进度为 0")
    void progressNone() {
        when(progressMapper.selectCount(any())).thenReturn(0L);
        assertEquals(0, courseService.calcProgress(4L, 1L, 5));
    }
}
