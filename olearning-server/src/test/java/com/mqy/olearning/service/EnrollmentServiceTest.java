package com.mqy.olearning.service;

import com.mqy.olearning.common.BusinessException;
import com.mqy.olearning.entity.Course;
import com.mqy.olearning.entity.Enrollment;
import com.mqy.olearning.mapper.CourseMapper;
import com.mqy.olearning.mapper.EnrollmentMapper;
import com.mqy.olearning.mapper.SysUserMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 选课业务单元测试（对应测试用例 TC-04 / TC-10）
 */
@ExtendWith(MockitoExtension.class)
class EnrollmentServiceTest {

    @Mock
    private EnrollmentMapper enrollmentMapper;
    @Mock
    private CourseMapper courseMapper;
    @Mock
    private SysUserMapper userMapper;
    @Mock
    private CourseService courseService;

    @InjectMocks
    private EnrollmentService enrollmentService;

    private Course publishedCourse() {
        Course course = new Course();
        course.setId(1L);
        course.setTeacherId(2L);
        course.setStatus(2);
        return course;
    }

    @Test
    @DisplayName("TC-04 重复选同一门课应抛出「已选过」业务异常")
    void enrollTwiceShouldFail() {
        when(courseMapper.selectById(1L)).thenReturn(publishedCourse());
        when(enrollmentMapper.selectCount(any())).thenReturn(1L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> enrollmentService.enroll(4L, 1L));
        assertTrue(ex.getMessage().contains("已选过"));
        verify(enrollmentMapper, never()).insert(any(Enrollment.class));
    }

    @Test
    @DisplayName("TC-10 并发下唯一索引兜底，DuplicateKeyException 转成业务提示")
    void concurrentDuplicateKeyShouldBeWrapped() {
        when(courseMapper.selectById(1L)).thenReturn(publishedCourse());
        when(enrollmentMapper.selectCount(any())).thenReturn(0L);
        when(enrollmentMapper.insert(any(Enrollment.class))).thenThrow(new DuplicateKeyException("uk_user_course"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> enrollmentService.enroll(4L, 1L));
        assertTrue(ex.getMessage().contains("已选过"));
    }

    @Test
    @DisplayName("未发布的课程不允许选课")
    void enrollUnpublishedCourseShouldFail() {
        Course draft = publishedCourse();
        draft.setStatus(0);
        when(courseMapper.selectById(1L)).thenReturn(draft);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> enrollmentService.enroll(4L, 1L));
        assertTrue(ex.getMessage().contains("未发布"));
    }
}
