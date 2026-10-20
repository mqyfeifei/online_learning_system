package com.mqy.olearning.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mqy.olearning.common.BusinessException;
import com.mqy.olearning.entity.Chapter;
import com.mqy.olearning.entity.Course;
import com.mqy.olearning.entity.CourseResource;
import com.mqy.olearning.entity.StudyProgress;
import com.mqy.olearning.mapper.ChapterMapper;
import com.mqy.olearning.mapper.CourseMapper;
import com.mqy.olearning.mapper.CourseResourceMapper;
import com.mqy.olearning.mapper.StudyProgressMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 章节与课件资源
 */
@Service
@RequiredArgsConstructor
public class ChapterService {

    private final ChapterMapper chapterMapper;
    private final CourseMapper courseMapper;
    private final CourseResourceMapper resourceMapper;
    private final StudyProgressMapper progressMapper;

    public List<Chapter> listByCourse(Long courseId) {
        return chapterMapper.selectList(new LambdaQueryWrapper<Chapter>()
                .eq(Chapter::getCourseId, courseId)
                .orderByAsc(Chapter::getSort));
    }

    public void save(Chapter chapter, Long teacherId) {
        checkOwner(chapter.getCourseId(), teacherId);
        if (chapter.getId() == null) {
            if (chapter.getSort() == null) {
                chapter.setSort(0);
            }
            chapterMapper.insert(chapter);
        } else {
            chapterMapper.updateById(chapter);
        }
    }

    public void remove(Long id, Long teacherId) {
        Chapter chapter = chapterMapper.selectById(id);
        if (chapter == null) {
            throw new BusinessException("章节不存在");
        }
        checkOwner(chapter.getCourseId(), teacherId);
        chapterMapper.deleteById(id);
        resourceMapper.delete(new LambdaQueryWrapper<CourseResource>()
                .eq(CourseResource::getChapterId, id));
        progressMapper.delete(new LambdaQueryWrapper<StudyProgress>()
                .eq(StudyProgress::getChapterId, id));
    }

    public List<CourseResource> resources(Long courseId, Long chapterId) {
        return resourceMapper.selectList(new LambdaQueryWrapper<CourseResource>()
                .eq(CourseResource::getCourseId, courseId)
                .eq(chapterId != null, CourseResource::getChapterId, chapterId)
                .orderByDesc(CourseResource::getId));
    }

    public void saveResource(CourseResource resource) {
        resourceMapper.insert(resource);
    }

    public void removeResource(Long id, Long teacherId) {
        CourseResource resource = resourceMapper.selectById(id);
        if (resource == null) {
            return;
        }
        checkOwner(resource.getCourseId(), teacherId);
        resourceMapper.deleteById(id);
    }

    /** 学生「学完本章」打卡，重复打卡不报错（唯一索引 + 先查后插） */
    public void finishChapter(Long userId, Long courseId, Long chapterId) {
        Long exists = progressMapper.selectCount(new LambdaQueryWrapper<StudyProgress>()
                .eq(StudyProgress::getUserId, userId)
                .eq(StudyProgress::getChapterId, chapterId));
        if (exists != null && exists > 0) {
            return;
        }
        StudyProgress progress = new StudyProgress();
        progress.setUserId(userId);
        progress.setCourseId(courseId);
        progress.setChapterId(chapterId);
        progress.setFinished(1);
        progress.setFinishTime(LocalDateTime.now());
        progressMapper.insert(progress);
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
