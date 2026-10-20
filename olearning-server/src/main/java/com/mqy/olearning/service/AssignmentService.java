package com.mqy.olearning.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mqy.olearning.common.BusinessException;
import com.mqy.olearning.entity.Assignment;
import com.mqy.olearning.entity.Course;
import com.mqy.olearning.entity.Submission;
import com.mqy.olearning.entity.SysUser;
import com.mqy.olearning.mapper.AssignmentMapper;
import com.mqy.olearning.mapper.CourseMapper;
import com.mqy.olearning.mapper.SubmissionMapper;
import com.mqy.olearning.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 作业：布置、提交、批阅
 */
@Service
@RequiredArgsConstructor
public class AssignmentService {

    private final AssignmentMapper assignmentMapper;
    private final SubmissionMapper submissionMapper;
    private final CourseMapper courseMapper;
    private final SysUserMapper userMapper;
    private final EnrollmentService enrollmentService;

    public List<Map<String, Object>> listByCourse(Long courseId, Long userId) {
        List<Assignment> list = assignmentMapper.selectList(new LambdaQueryWrapper<Assignment>()
                .eq(Assignment::getCourseId, courseId)
                .orderByDesc(Assignment::getCreateTime));
        List<Map<String, Object>> result = new ArrayList<>();
        for (Assignment a : list) {
            Map<String, Object> item = new HashMap<>();
            item.put("assignment", a);
            if (userId != null) {
                Submission sub = submissionMapper.selectOne(new LambdaQueryWrapper<Submission>()
                        .eq(Submission::getAssignmentId, a.getId())
                        .eq(Submission::getUserId, userId));
                item.put("submission", sub);
            }
            result.add(item);
        }
        return result;
    }

    public void save(Assignment assignment, Long teacherId) {
        checkOwner(assignment.getCourseId(), teacherId);
        if (assignment.getId() == null) {
            assignmentMapper.insert(assignment);
        } else {
            assignmentMapper.updateById(assignment);
        }
    }

    public void remove(Long id, Long teacherId) {
        Assignment assignment = assignmentMapper.selectById(id);
        if (assignment == null) {
            return;
        }
        checkOwner(assignment.getCourseId(), teacherId);
        assignmentMapper.deleteById(id);
        submissionMapper.delete(new LambdaQueryWrapper<Submission>()
                .eq(Submission::getAssignmentId, id));
    }

    /** 学生提交：必须先选课、且未过截止时间；重复提交视为修改 */
    public void submit(Long userId, Long assignmentId, String content, String attachUrl) {
        Assignment assignment = assignmentMapper.selectById(assignmentId);
        if (assignment == null) {
            throw new BusinessException("作业不存在");
        }
        if (!enrollmentService.isEnrolled(userId, assignment.getCourseId())) {
            throw new BusinessException("请先选修该课程");
        }
        if (assignment.getDeadline() != null && LocalDateTime.now().isAfter(assignment.getDeadline())) {
            throw new BusinessException("作业已截止，无法提交");
        }

        Submission exists = submissionMapper.selectOne(new LambdaQueryWrapper<Submission>()
                .eq(Submission::getAssignmentId, assignmentId)
                .eq(Submission::getUserId, userId));

        if (exists == null) {
            Submission submission = new Submission();
            submission.setAssignmentId(assignmentId);
            submission.setUserId(userId);
            submission.setContent(content);
            submission.setAttachUrl(attachUrl);
            submission.setStatus(0);
            submission.setSubmitTime(LocalDateTime.now());
            try {
                submissionMapper.insert(submission);
            } catch (DuplicateKeyException e) {
                throw new BusinessException("本作业已提交过，请使用修改功能");
            }
        } else {
            if (exists.getStatus() != null && exists.getStatus() == 1) {
                throw new BusinessException("作业已批阅，不能再修改");
            }
            exists.setContent(content);
            exists.setAttachUrl(attachUrl);
            exists.setSubmitTime(LocalDateTime.now());
            submissionMapper.updateById(exists);
        }
    }

    /** 教师端：某次作业的全部提交 */
    public List<Map<String, Object>> submissions(Long assignmentId, Long teacherId) {
        Assignment assignment = assignmentMapper.selectById(assignmentId);
        if (assignment == null) {
            throw new BusinessException("作业不存在");
        }
        checkOwner(assignment.getCourseId(), teacherId);

        List<Submission> list = submissionMapper.selectList(new LambdaQueryWrapper<Submission>()
                .eq(Submission::getAssignmentId, assignmentId)
                .orderByDesc(Submission::getSubmitTime));
        List<Map<String, Object>> result = new ArrayList<>();
        for (Submission s : list) {
            SysUser user = userMapper.selectById(s.getUserId());
            Map<String, Object> item = new HashMap<>();
            item.put("submission", s);
            item.put("realName", user == null ? "" : user.getRealName());
            item.put("username", user == null ? "" : user.getUsername());
            result.add(item);
        }
        return result;
    }

    /** 批阅打分 */
    public void grade(Long submissionId, Integer score, String comment, Long teacherId) {
        Submission submission = submissionMapper.selectById(submissionId);
        if (submission == null) {
            throw new BusinessException("提交记录不存在");
        }
        Assignment assignment = assignmentMapper.selectById(submission.getAssignmentId());
        checkOwner(assignment.getCourseId(), teacherId);

        if (score == null || score < 0 || score > 100) {
            throw new BusinessException("分数需在 0-100 之间");
        }
        submission.setScore(score);
        submission.setComment(comment);
        submission.setStatus(1);
        submissionMapper.updateById(submission);
    }

    /** 我的成绩：作业 + 测验 */
    public List<Map<String, Object>> myGrades(Long userId) {
        List<Submission> subs = submissionMapper.selectList(new LambdaQueryWrapper<Submission>()
                .eq(Submission::getUserId, userId)
                .isNotNull(Submission::getScore));
        List<Map<String, Object>> result = new ArrayList<>();
        for (Submission s : subs) {
            Assignment a = assignmentMapper.selectById(s.getAssignmentId());
            result.add(Map.of(
                    "type", "作业",
                    "title", a == null ? "已删除作业" : a.getTitle(),
                    "score", s.getScore(),
                    "comment", s.getComment() == null ? "" : s.getComment(),
                    "time", s.getSubmitTime() == null ? "" : s.getSubmitTime().toString()
            ));
        }
        return result;
    }

    private void checkOwner(Long courseId, Long teacherId) {
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new BusinessException("课程不存在");
        }
        if (teacherId != null && !teacherId.equals(course.getTeacherId())) {
            throw new BusinessException(403, "只能操作自己的课程");
        }
    }
}
