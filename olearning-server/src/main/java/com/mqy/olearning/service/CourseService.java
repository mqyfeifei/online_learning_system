package com.mqy.olearning.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mqy.olearning.common.BusinessException;
import com.mqy.olearning.common.PageResult;
import com.mqy.olearning.dto.CourseDTO;
import com.mqy.olearning.dto.CourseVO;
import com.mqy.olearning.entity.*;
import com.mqy.olearning.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 课程业务：广场查询、详情、教师端 CRUD、管理员审核
 */
@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseMapper courseMapper;
    private final ChapterMapper chapterMapper;
    private final EnrollmentMapper enrollmentMapper;
    private final StudyProgressMapper progressMapper;
    private final SysUserMapper userMapper;
    private final CourseCategoryMapper categoryMapper;
    private final CourseResourceMapper resourceMapper;

    /** 课程广场：只查已发布(status=2)；sort = hot 按浏览量，new 按创建时间 */
    public PageResult<CourseVO> publicPage(Long categoryId, String keyword, String sort,
                                           long current, long size, Long userId) {
        LambdaQueryWrapper<Course> wrapper = new LambdaQueryWrapper<Course>()
                .eq(Course::getStatus, 2)
                .in(categoryId != null, Course::getCategoryId, categoryAndChildren(categoryId))
                .like(StringUtils.hasText(keyword), Course::getTitle, keyword);

        if ("hot".equals(sort)) {
            wrapper.orderByDesc(Course::getViewCount);
        } else {
            wrapper.orderByDesc(Course::getCreateTime);
        }

        Page<Course> page = courseMapper.selectPage(new Page<>(current, size), wrapper);
        List<CourseVO> list = page.getRecords().stream()
                .map(c -> toVO(c, userId))
                .collect(Collectors.toList());

        PageResult<CourseVO> result = new PageResult<>();
        result.setRecords(list);
        result.setTotal(page.getTotal());
        result.setCurrent(page.getCurrent());
        result.setSize(page.getSize());
        return result;
    }

    /** 课程详情：课程信息 + 章节目录 + 当前用户进度 */
    public Map<String, Object> detail(Long courseId, Long userId) {
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new BusinessException("课程不存在");
        }
        // 浏览量 +1（简单累计，不做并发控制，报告里注明）
        course.setViewCount(course.getViewCount() == null ? 1 : course.getViewCount() + 1);
        courseMapper.updateById(course);

        List<Chapter> chapters = chapterMapper.selectList(new LambdaQueryWrapper<Chapter>()
                .eq(Chapter::getCourseId, courseId)
                .orderByAsc(Chapter::getSort));

        Map<String, Object> data = new HashMap<>();
        data.put("course", toVO(course, userId));
        data.put("chapters", chapters);
        if (userId != null) {
            List<Long> finished = progressMapper.selectList(new LambdaQueryWrapper<StudyProgress>()
                            .eq(StudyProgress::getUserId, userId)
                            .eq(StudyProgress::getCourseId, courseId)
                            .eq(StudyProgress::getFinished, 1))
                    .stream().map(StudyProgress::getChapterId).collect(Collectors.toList());
            data.put("finishedChapterIds", finished);
        }
        return data;
    }

    /** 教师端：我的课程列表，可按状态过滤 */
    public List<CourseVO> teacherCourses(Long teacherId, Integer status) {
        List<Course> courses = courseMapper.selectList(new LambdaQueryWrapper<Course>()
                .eq(Course::getTeacherId, teacherId)
                .eq(status != null, Course::getStatus, status)
                .orderByDesc(Course::getCreateTime));
        return courses.stream().map(c -> toVO(c, null)).collect(Collectors.toList());
    }

    public Long save(CourseDTO dto, Long teacherId) {
        Course course = new Course();
        course.setTitle(dto.getTitle());
        course.setCategoryId(dto.getCategoryId());
        course.setCover(dto.getCover());
        course.setDescription(dto.getDescription());

        if (dto.getId() == null) {
            course.setTeacherId(teacherId);
            course.setStatus(0);
            course.setViewCount(0);
            courseMapper.insert(course);
            return course.getId();
        }

        Course old = courseMapper.selectById(dto.getId());
        if (old == null) {
            throw new BusinessException("课程不存在");
        }
        if (!teacherId.equals(old.getTeacherId())) {
            throw new BusinessException(403, "只能编辑自己的课程");
        }
        course.setId(dto.getId());
        courseMapper.updateById(course);
        return course.getId();
    }

    /** 教师提交审核：0草稿/4驳回 → 1待审核 */
    public void submitAudit(Long courseId, Long teacherId) {
        Course course = requireOwn(courseId, teacherId);
        if (course.getStatus() == 2) {
            throw new BusinessException("课程已发布，无需再次提交");
        }
        Course update = new Course();
        update.setId(courseId);
        update.setStatus(1);
        update.setRejectReason(null);
        courseMapper.updateById(update);
    }

    /** 教师下架 */
    public void offline(Long courseId, Long teacherId) {
        requireOwn(courseId, teacherId);
        Course update = new Course();
        update.setId(courseId);
        update.setStatus(3);
        courseMapper.updateById(update);
    }

    /** 管理员审核：pass=true → 已发布(2)；false → 驳回(4) 并记录理由 */
    public void audit(Long courseId, boolean pass, String reason) {
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new BusinessException("课程不存在");
        }
        Course update = new Course();
        update.setId(courseId);
        update.setStatus(pass ? 2 : 4);
        update.setRejectReason(pass ? null : reason);
        courseMapper.updateById(update);
    }

    /** 管理员审核列表：默认查待审核 */
    public PageResult<CourseVO> auditPage(Integer status, long current, long size) {
        Page<Course> page = courseMapper.selectPage(new Page<>(current, size),
                new LambdaQueryWrapper<Course>()
                        .eq(status != null, Course::getStatus, status)
                        .orderByDesc(Course::getCreateTime));
        PageResult<CourseVO> result = new PageResult<>();
        result.setRecords(page.getRecords().stream().map(c -> toVO(c, null)).collect(Collectors.toList()));
        result.setTotal(page.getTotal());
        result.setCurrent(page.getCurrent());
        result.setSize(page.getSize());
        return result;
    }

    @Transactional
    public void remove(Long courseId, Long teacherId) {
        Course course = requireOwn(courseId, teacherId);
        if (course.getStatus() == 2) {
            throw new BusinessException("已发布课程请先下架再删除");
        }
        courseMapper.deleteById(courseId);
        chapterMapper.delete(new LambdaQueryWrapper<Chapter>().eq(Chapter::getCourseId, courseId));
        resourceMapper.delete(new LambdaQueryWrapper<CourseResource>().eq(CourseResource::getCourseId, courseId));
    }

    /** 选课人数、章节数、进度等展示字段在这里补齐 */
    public CourseVO toVO(Course course, Long userId) {
        CourseVO vo = new CourseVO();
        org.springframework.beans.BeanUtils.copyProperties(course, vo);

        SysUser teacher = userMapper.selectById(course.getTeacherId());
        vo.setTeacherName(teacher == null ? "未知教师" : teacher.getRealName());
        CourseCategory category = course.getCategoryId() == null ? null : categoryMapper.selectById(course.getCategoryId());
        vo.setCategoryName(category == null ? "未分类" : category.getName());

        Long chapterCount = chapterMapper.selectCount(new LambdaQueryWrapper<Chapter>()
                .eq(Chapter::getCourseId, course.getId()));
        vo.setChapterCount(chapterCount == null ? 0 : chapterCount.intValue());

        Long studentCount = enrollmentMapper.selectCount(new LambdaQueryWrapper<Enrollment>()
                .eq(Enrollment::getCourseId, course.getId())
                .eq(Enrollment::getStatus, 1));
        vo.setStudentCount(studentCount == null ? 0 : studentCount.intValue());

        if (userId != null) {
            Long enrolled = enrollmentMapper.selectCount(new LambdaQueryWrapper<Enrollment>()
                    .eq(Enrollment::getUserId, userId)
                    .eq(Enrollment::getCourseId, course.getId())
                    .eq(Enrollment::getStatus, 1));
            vo.setEnrolled(enrolled != null && enrolled > 0);
            vo.setProgress(calcProgress(userId, course.getId(), vo.getChapterCount()));
        }
        return vo;
    }

    public int calcProgress(Long userId, Long courseId, int totalChapters) {
        if (totalChapters == 0) {
            return 0;
        }
        Long finished = progressMapper.selectCount(new LambdaQueryWrapper<StudyProgress>()
                .eq(StudyProgress::getUserId, userId)
                .eq(StudyProgress::getCourseId, courseId)
                .eq(StudyProgress::getFinished, 1));
        return (int) Math.round((finished == null ? 0 : finished) * 100.0 / totalChapters);
    }

    /** 选一级分类时，把它的子分类一起纳入过滤 */
    private List<Long> categoryAndChildren(Long categoryId) {
        List<Long> ids = new ArrayList<>();
        ids.add(categoryId);
        categoryMapper.selectList(new LambdaQueryWrapper<CourseCategory>()
                        .eq(CourseCategory::getParentId, categoryId))
                .forEach(c -> ids.add(c.getId()));
        return ids;
    }

    private Course requireOwn(Long courseId, Long teacherId) {
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new BusinessException("课程不存在");
        }
        if (!course.getTeacherId().equals(teacherId)) {
            throw new BusinessException(403, "只能操作自己的课程");
        }
        return course;
    }
}
