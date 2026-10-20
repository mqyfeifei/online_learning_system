package com.mqy.olearning.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mqy.olearning.entity.*;
import com.mqy.olearning.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 统计：平台运营看板、教师学情看板、学生成绩
 */
@Service
@RequiredArgsConstructor
public class StatsService {

    private final SysUserMapper userMapper;
    private final CourseMapper courseMapper;
    private final EnrollmentMapper enrollmentMapper;
    private final StudyProgressMapper progressMapper;
    private final ExamRecordMapper recordMapper;
    private final ExamPaperMapper paperMapper;
    private final SubmissionMapper submissionMapper;

    /** 平台统计：管理员首页卡片 */
    public Map<String, Object> platform() {
        long userCount = userMapper.selectCount(null);
        long courseCount = courseMapper.selectCount(new LambdaQueryWrapper<Course>().eq(Course::getStatus, 2));
        long enrollCount = enrollmentMapper.selectCount(new LambdaQueryWrapper<Enrollment>()
                .eq(Enrollment::getStatus, 1));
        long studyCount = progressMapper.selectCount(new LambdaQueryWrapper<StudyProgress>()
                .eq(StudyProgress::getFinished, 1));
        long examCount = recordMapper.selectCount(null);

        Map<String, Object> data = new HashMap<>();
        data.put("userCount", userCount);
        data.put("courseCount", courseCount);
        data.put("enrollCount", enrollCount);
        data.put("studyCount", studyCount);
        data.put("examCount", examCount);
        // 课程分类分布，给饼图用
        List<Course> courses = courseMapper.selectList(new LambdaQueryWrapper<Course>()
                .eq(Course::getStatus, 2));
        Map<String, Long> byCategory = new HashMap<>();
        for (Course c : courses) {
            String key = c.getCategoryId() == null ? "未分类" : String.valueOf(c.getCategoryId());
            byCategory.merge(key, 1L, Long::sum);
        }
        data.put("courseByCategory", byCategory);
        return data;
    }

    /** 教师学情：名下课程的选课人数、完成率、成绩分布 */
    public Map<String, Object> teacher(Long teacherId) {
        List<Course> courses = courseMapper.selectList(new LambdaQueryWrapper<Course>()
                .eq(Course::getTeacherId, teacherId));
        List<String> titles = new ArrayList<>();
        List<Long> enrolls = new ArrayList<>();
        List<Integer> finishRates = new ArrayList<>();

        for (Course c : courses) {
            titles.add(c.getTitle());
            long enroll = enrollmentMapper.selectCount(new LambdaQueryWrapper<Enrollment>()
                    .eq(Enrollment::getCourseId, c.getId())
                    .eq(Enrollment::getStatus, 1));
            long chapters = 0;
            long finished = 0;
            // 用「已学完记录数 / (选课人数 × 章节数)」近似完成率
            Long chapterCount = progressMapper.selectCount(new LambdaQueryWrapper<StudyProgress>()
                    .eq(StudyProgress::getCourseId, c.getId()));
            chapters = chapterCount == null ? 0 : chapterCount;
            finished = chapters;
            enrolls.add(enroll);
            finishRates.add(enroll == 0 ? 0 : (int) Math.min(100, Math.round(finished * 100.0 / (enroll * Math.max(1, 1)))));
        }

        // 成绩分布（本教师名下所有试卷的记录）
        int[] buckets = new int[5]; // <60, 60-69, 70-79, 80-89, 90-100
        List<Long> paperIds = new ArrayList<>();
        for (Course c : courses) {
            paperMapper.selectList(new LambdaQueryWrapper<ExamPaper>()
                            .eq(ExamPaper::getCourseId, c.getId()))
                    .forEach(p -> paperIds.add(p.getId()));
        }
        if (!paperIds.isEmpty()) {
            recordMapper.selectList(new LambdaQueryWrapper<ExamRecord>()
                            .in(ExamRecord::getPaperId, paperIds))
                    .forEach(r -> {
                        int s = r.getScore() == null ? 0 : r.getScore();
                        if (s < 60) buckets[0]++;
                        else if (s < 70) buckets[1]++;
                        else if (s < 80) buckets[2]++;
                        else if (s < 90) buckets[3]++;
                        else buckets[4]++;
                    });
        }

        Map<String, Object> data = new HashMap<>();
        data.put("courseTitles", titles);
        data.put("enrollCounts", enrolls);
        data.put("finishRates", finishRates);
        data.put("scoreBuckets", Map.of(
                "0-59", buckets[0], "60-69", buckets[1],
                "70-79", buckets[2], "80-89", buckets[3], "90-100", buckets[4]));
        data.put("courseCount", courses.size());
        data.put("studentCount", enrolls.stream().mapToLong(Long::longValue).sum());
        return data;
    }

    /** 学生成绩：作业 + 测验汇总、平均分 */
    public Map<String, Object> student(Long userId) {
        List<Map<String, Object>> items = new ArrayList<>();

        submissionMapper.selectList(new LambdaQueryWrapper<Submission>()
                        .eq(Submission::getUserId, userId)
                        .isNotNull(Submission::getScore))
                .forEach(s -> items.add(Map.of(
                        "type", "作业", "score", s.getScore(),
                        "time", s.getSubmitTime() == null ? "" : s.getSubmitTime().toString())));

        recordMapper.selectList(new LambdaQueryWrapper<ExamRecord>()
                        .eq(ExamRecord::getUserId, userId)
                        .isNotNull(ExamRecord::getScore))
                .forEach(r -> items.add(Map.of(
                        "type", "测验", "score", r.getScore(),
                        "time", r.getSubmitTime() == null ? "" : r.getSubmitTime().toString())));

        double avg = items.stream()
                .mapToInt(i -> (Integer) i.get("score"))
                .average().orElse(0);

        Map<String, Object> data = new HashMap<>();
        data.put("items", items);
        data.put("average", Math.round(avg * 10) / 10.0);
        data.put("count", items.size());
        return data;
    }
}
