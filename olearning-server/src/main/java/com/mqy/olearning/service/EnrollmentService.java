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
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 选课：一人一课唯一约束由数据库唯一索引 + DuplicateKeyException 兜底。
 * <p>v3 起支持「选课审核」：课程开启审核后，学生选课进入待审核(2)状态，
 * 教师审批通过才变成在读(1)。
 */
@Service
@RequiredArgsConstructor
public class EnrollmentService {

    /** 学生选课结果 */
    public static final String RESULT_ENROLLED = "ENROLLED";
    public static final String RESULT_PENDING = "PENDING";

    private final EnrollmentMapper enrollmentMapper;
    private final CourseMapper courseMapper;
    private final SysUserMapper userMapper;
    private final CourseService courseService;

    /** 选课：返回 ENROLLED（直接选上）或 PENDING（已提交审核） */
    public String enroll(Long userId, Long courseId) {
        Course course = courseMapper.selectById(courseId);
        if (course == null || course.getStatus() != 2) {
            throw new BusinessException("课程不存在或未发布");
        }
        Enrollment exists = enrollmentMapper.selectOne(new LambdaQueryWrapper<Enrollment>()
                .eq(Enrollment::getUserId, userId)
                .eq(Enrollment::getCourseId, courseId)
                .last("limit 1"));

        boolean needAudit = course.getEnrollAudit() != null && course.getEnrollAudit() == 1;
        int targetStatus = needAudit ? 2 : 1;

        if (exists != null) {
            int status = exists.getStatus() == null ? 0 : exists.getStatus();
            if (status == 1) {
                throw new BusinessException("你已选过该课程");
            }
            if (status == 2) {
                throw new BusinessException("选课申请已提交，请等待教师审核");
            }
            // 退课(0) / 被拒(3) → 重新申请
            Enrollment update = new Enrollment();
            update.setId(exists.getId());
            update.setStatus(targetStatus);
            enrollmentMapper.updateById(update);
            return needAudit ? RESULT_PENDING : RESULT_ENROLLED;
        }

        Enrollment enrollment = new Enrollment();
        enrollment.setUserId(userId);
        enrollment.setCourseId(courseId);
        enrollment.setStatus(targetStatus);
        try {
            enrollmentMapper.insert(enrollment);
        } catch (DuplicateKeyException e) {
            // 并发下唯一索引兜底，保证不出现重复选课
            throw new BusinessException("你已选过该课程");
        }
        return needAudit ? RESULT_PENDING : RESULT_ENROLLED;
    }

    public void cancel(Long userId, Long courseId) {
        Enrollment enrollment = enrollmentMapper.selectOne(new LambdaQueryWrapper<Enrollment>()
                .eq(Enrollment::getUserId, userId)
                .eq(Enrollment::getCourseId, courseId)
                .last("limit 1"));
        if (enrollment == null) {
            throw new BusinessException("未选该课程");
        }
        enrollmentMapper.deleteById(enrollment.getId());
    }

    /** 我的课程：带上学习进度，含待审核的申请 */
    public List<CourseVO> myCourses(Long userId) {
        List<Enrollment> enrollments = enrollmentMapper.selectList(new LambdaQueryWrapper<Enrollment>()
                .eq(Enrollment::getUserId, userId)
                .in(Enrollment::getStatus, 1, 2));
        if (enrollments.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> courseIds = enrollments.stream().map(Enrollment::getCourseId).collect(Collectors.toList());
        return courseMapper.selectBatchIds(courseIds).stream()
                .map(c -> courseService.toVO(c, userId))
                .collect(Collectors.toList());
    }

    /** 教师端：待审核的选课申请 */
    public List<Map<String, Object>> pendingApplies(Long courseId, Long teacherId) {
        requireOwn(courseId, teacherId);
        List<Enrollment> list = enrollmentMapper.selectList(new LambdaQueryWrapper<Enrollment>()
                .eq(Enrollment::getCourseId, courseId)
                .eq(Enrollment::getStatus, 2)
                .orderByAsc(Enrollment::getId));
        List<Map<String, Object>> result = new ArrayList<>();
        for (Enrollment e : list) {
            SysUser user = userMapper.selectById(e.getUserId());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", e.getId());
            item.put("userId", e.getUserId());
            item.put("username", user == null ? "" : user.getUsername());
            item.put("realName", user == null ? "" : nullToEmpty(user.getRealName()));
            item.put("phone", user == null ? "" : nullToEmpty(user.getPhone()));
            item.put("applyTime", e.getCreateTime() == null ? "" : e.getCreateTime().toString());
            result.add(item);
        }
        return result;
    }

    /** 教师审批选课申请：pass=true 通过，false 拒绝 */
    public void auditApply(Long enrollmentId, boolean pass, Long teacherId) {
        Enrollment enrollment = enrollmentMapper.selectById(enrollmentId);
        if (enrollment == null) {
            throw new BusinessException("申请不存在");
        }
        requireOwn(enrollment.getCourseId(), teacherId);
        if (enrollment.getStatus() == null || enrollment.getStatus() != 2) {
            throw new BusinessException("该申请已处理");
        }
        Enrollment update = new Enrollment();
        update.setId(enrollmentId);
        update.setStatus(pass ? 1 : 3);
        enrollmentMapper.updateById(update);
    }

    /**
     * 教师导入学生名单：每行一个学号（也允许「学号,姓名」两列，取第一列）。
     * 学号匹配不到时退回按用户名匹配，兼容历史名单。
     * 返回逐行结果，前端展示成功 / 失败明细。
     */
    @Transactional
    public Map<String, Object> importStudents(Long courseId, Long teacherId, String rawText) {
        requireOwn(courseId, teacherId);
        if (rawText == null || rawText.isBlank()) {
            throw new BusinessException("导入内容为空");
        }
        List<Map<String, Object>> success = new ArrayList<>();
        List<Map<String, Object>> failed = new ArrayList<>();
        String[] lines = rawText.split("\\R");
        int lineNo = 0;
        for (String line : lines) {
            lineNo++;
            String text = line.trim();
            if (text.isEmpty() || text.startsWith("#")) {
                continue;
            }
            String key = text.split("[,，\\t;；]")[0].trim();
            if (key.isEmpty()) {
                continue;
            }
            SysUser user = findUser(key);
            if (user == null) {
                failed.add(fail(lineNo, key, "账号不存在，请先让学生注册"));
                continue;
            }
            if (!"STUDENT".equals(user.getRole())) {
                failed.add(fail(lineNo, key, "该账号不是学生角色"));
                continue;
            }
            if (user.getStatus() != null && user.getStatus() == 0) {
                failed.add(fail(lineNo, key, "账号已被禁用"));
                continue;
            }
            Enrollment exists = enrollmentMapper.selectOne(new LambdaQueryWrapper<Enrollment>()
                    .eq(Enrollment::getUserId, user.getId())
                    .eq(Enrollment::getCourseId, courseId)
                    .last("limit 1"));
            if (exists != null && exists.getStatus() != null
                    && (exists.getStatus() == 1 || exists.getStatus() == 2)) {
                Map<String, Object> item = row(lineNo, user);
                item.put("note", "已在名单中");
                success.add(item);
                continue;
            }
            if (exists != null) {
                Enrollment update = new Enrollment();
                update.setId(exists.getId());
                update.setStatus(1);
                enrollmentMapper.updateById(update);
            } else {
                Enrollment enrollment = new Enrollment();
                enrollment.setUserId(user.getId());
                enrollment.setCourseId(courseId);
                enrollment.setStatus(1);
                try {
                    enrollmentMapper.insert(enrollment);
                } catch (DuplicateKeyException e) {
                    failed.add(fail(lineNo, key, "并发写入冲突，请重试"));
                    continue;
                }
            }
            success.add(row(lineNo, user));
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("success", success);
        data.put("failed", failed);
        data.put("successCount", success.size());
        data.put("failedCount", failed.size());
        return data;
    }

    /** 教师端：本课程学生名单 + 各自进度 */
    public List<Map<String, Object>> students(Long courseId, Long teacherId) {
        requireOwn(courseId, teacherId);
        List<Enrollment> list = enrollmentMapper.selectList(new LambdaQueryWrapper<Enrollment>()
                .eq(Enrollment::getCourseId, courseId)
                .eq(Enrollment::getStatus, 1));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Enrollment e : list) {
            SysUser user = userMapper.selectById(e.getUserId());
            CourseVO vo = courseService.toVO(courseMapper.selectById(courseId), e.getUserId());
            result.add(Map.of(
                    "userId", e.getUserId(),
                    "username", user == null ? "" : user.getUsername(),
                    "userNo", user == null ? "" : nullToEmpty(user.getUserNo()),
                    "realName", user == null ? "" : nullToEmpty(user.getRealName()),
                    "phone", user == null ? "" : nullToEmpty(user.getPhone()),
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

    /** 课程当前状态的选课人数（只统计在读） */
    public long countEnrolled(Long courseId) {
        Long count = enrollmentMapper.selectCount(new LambdaQueryWrapper<Enrollment>()
                .eq(Enrollment::getCourseId, courseId)
                .eq(Enrollment::getStatus, 1));
        return count == null ? 0 : count;
    }

    // ---------------- 工具 ----------------

    private void requireOwn(Long courseId, Long teacherId) {
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new BusinessException("课程不存在");
        }
        if (teacherId != null && !teacherId.equals(course.getTeacherId())) {
            throw new BusinessException(403, "只能操作自己的课程");
        }
    }

    private Map<String, Object> fail(int line, String username, String reason) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("line", line);
        item.put("username", username);
        item.put("reason", reason);
        return item;
    }

    /** 按学号查，查不到再按用户名查（兼容旧名单） */
    private SysUser findUser(String key) {
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUserNo, key)
                .last("limit 1"));
        if (user == null) {
            user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                    .eq(SysUser::getUsername, key)
                    .last("limit 1"));
        }
        return user;
    }

    private Map<String, Object> row(int line, SysUser user) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("line", line);
        item.put("username", user.getUsername());
        item.put("userNo", nullToEmpty(user.getUserNo()));
        item.put("realName", nullToEmpty(user.getRealName()));
        return item;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
