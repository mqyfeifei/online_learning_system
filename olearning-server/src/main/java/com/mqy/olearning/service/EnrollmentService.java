package com.mqy.olearning.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mqy.olearning.common.BusinessException;
import com.mqy.olearning.dto.CourseVO;
import com.mqy.olearning.entity.Course;
import com.mqy.olearning.entity.Enrollment;
import com.mqy.olearning.entity.SysUser;
import com.mqy.olearning.mapper.CourseMapper;
import com.mqy.olearning.mapper.EnrollmentMapper;
import com.mqy.olearning.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 选课：一人一课唯一约束由数据库唯一索引 + DuplicateKeyException 兜底
 */
@Service
@RequiredArgsConstructor
public class EnrollmentService {

    private final EnrollmentMapper enrollmentMapper;
    private final CourseMapper courseMapper;
    private final SysUserMapper userMapper;
    private final CourseService courseService;

    public void enroll(Long userId, Long courseId) {
        Course course = courseMapper.selectById(courseId);
        if (course == null || course.getStatus() != 2) {
            throw new BusinessException("课程不存在或未发布");
        }
        Long exists = enrollmentMapper.selectCount(new LambdaQueryWrapper<Enrollment>()
                .eq(Enrollment::getUserId, userId)
                .eq(Enrollment::getCourseId, courseId));
        if (exists != null && exists > 0) {
            throw new BusinessException("你已选过该课程");
        }
        Enrollment enrollment = new Enrollment();
        enrollment.setUserId(userId);
        enrollment.setCourseId(courseId);
        enrollment.setStatus(1);
        try {
            enrollmentMapper.insert(enrollment);
        } catch (DuplicateKeyException e) {
            // 并发下唯一索引兜底，保证不出现重复选课
            throw new BusinessException("你已选过该课程");
        }
    }

    public void cancel(Long userId, Long courseId) {
        Enrollment enrollment = enrollmentMapper.selectOne(new LambdaQueryWrapper<Enrollment>()
                .eq(Enrollment::getUserId, userId)
                .eq(Enrollment::getCourseId, courseId));
        if (enrollment == null) {
            throw new BusinessException("未选该课程");
        }
        enrollmentMapper.deleteById(enrollment.getId());
    }

    /** 我的课程：带上学习进度 */
    public List<CourseVO> myCourses(Long userId) {
        List<Long> courseIds = enrollmentMapper.selectList(new LambdaQueryWrapper<Enrollment>()
                        .eq(Enrollment::getUserId, userId)
                        .eq(Enrollment::getStatus, 1))
                .stream().map(Enrollment::getCourseId).collect(Collectors.toList());
        if (courseIds.isEmpty()) {
            return new ArrayList<>();
        }
        return courseMapper.selectBatchIds(courseIds).stream()
                .map(c -> courseService.toVO(c, userId))
                .collect(Collectors.toList());
    }

    /** 教师端：本课程学生名单 + 各自进度 */
    public List<Map<String, Object>> students(Long courseId, Long teacherId) {
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new BusinessException("课程不存在");
        }
        if (!course.getTeacherId().equals(teacherId)) {
            throw new BusinessException(403, "只能查看自己课程的学生");
        }
        List<Enrollment> list = enrollmentMapper.selectList(new LambdaQueryWrapper<Enrollment>()
                .eq(Enrollment::getCourseId, courseId)
                .eq(Enrollment::getStatus, 1));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Enrollment e : list) {
            SysUser user = userMapper.selectById(e.getUserId());
            CourseVO vo = courseService.toVO(course, e.getUserId());
            result.add(Map.of(
                    "userId", e.getUserId(),
                    "username", user == null ? "" : user.getUsername(),
                    "realName", user == null ? "" : (user.getRealName() == null ? "" : user.getRealName()),
                    "phone", user == null ? "" : (user.getPhone() == null ? "" : user.getPhone()),
                    "progress", vo.getProgress() == null ? 0 : vo.getProgress(),
                    "enrollTime", e.getCreateTime() == null ? "" : e.getCreateTime().toString()
            ));
        }
        return result;
    }

    public boolean isEnrolled(Long userId, Long courseId) {
        Long count = enrollmentMapper.selectCount(new LambdaQueryWrapper<Enrollment>()
                .eq(Enrollment::getUserId, userId)
                .eq(Enrollment::getCourseId, courseId)
                .eq(Enrollment::getStatus, 1));
        return count != null && count > 0;
    }
}
