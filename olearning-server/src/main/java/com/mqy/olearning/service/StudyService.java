package com.mqy.olearning.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mqy.olearning.common.BusinessException;
import com.mqy.olearning.dto.CourseVO;
import com.mqy.olearning.entity.*;
import com.mqy.olearning.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 学习增强能力：课程收藏、学习笔记、学习概览统计
 */
@Service
@RequiredArgsConstructor
public class StudyService {

    private static final DateTimeFormatter MD = DateTimeFormatter.ofPattern("MM-dd");
    private static final String[] WEEK_CN = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};

    private final FavoriteMapper favoriteMapper;
    private final StudyNoteMapper noteMapper;
    private final EnrollmentMapper enrollmentMapper;
    private final CourseMapper courseMapper;
    private final ChapterMapper chapterMapper;
    private final StudyProgressMapper progressMapper;
    private final ExamRecordMapper recordMapper;
    private final SubmissionMapper submissionMapper;
    private final AssignmentMapper assignmentMapper;
    private final CourseService courseService;

    // ==================== 收藏 ====================

    /** 收藏 / 取消收藏，返回操作后的状态：true=已收藏 */
    public boolean toggleFavorite(Long userId, Long courseId) {
        if (courseMapper.selectById(courseId) == null) {
            throw new BusinessException("课程不存在");
        }
        Favorite exists = favoriteMapper.selectOne(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getCourseId, courseId));
        if (exists != null) {
            favoriteMapper.deleteById(exists.getId());
            return false;
        }
        Favorite favorite = new Favorite();
        favorite.setUserId(userId);
        favorite.setCourseId(courseId);
        try {
            favoriteMapper.insert(favorite);
        } catch (DuplicateKeyException e) {
            // 并发下唯一索引兜底：重复收藏视为已收藏，不报错
            return true;
        }
        return true;
    }

    public boolean isFavorite(Long userId, Long courseId) {
        if (userId == null) {
            return false;
        }
        Long count = favoriteMapper.selectCount(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getCourseId, courseId));
        return count != null && count > 0;
    }

    /** 我的收藏（带课程展示字段与学习进度） */
    public List<CourseVO> myFavorites(Long userId) {
        List<Long> courseIds = favoriteMapper.selectList(new LambdaQueryWrapper<Favorite>()
                        .eq(Favorite::getUserId, userId)
                        .orderByDesc(Favorite::getId))
                .stream().map(Favorite::getCourseId).collect(Collectors.toList());
        if (courseIds.isEmpty()) {
            return new ArrayList<>();
        }
        // selectBatchIds 不保证顺序，按收藏时间倒序重排
        Map<Long, Course> byId = courseMapper.selectBatchIds(courseIds).stream()
                .collect(Collectors.toMap(Course::getId, c -> c));
        List<CourseVO> result = new ArrayList<>();
        for (Long id : courseIds) {
            Course c = byId.get(id);
            if (c != null) {
                result.add(courseService.toVO(c, userId));
            }
        }
        return result;
    }

    // ==================== 笔记 ====================

    /** 我的笔记，可按课程过滤；带课程名与章节名 */
    public List<Map<String, Object>> myNotes(Long userId, Long courseId) {
        LambdaQueryWrapper<StudyNote> wrapper = new LambdaQueryWrapper<StudyNote>()
                .eq(StudyNote::getUserId, userId)
                .orderByDesc(StudyNote::getId);
        if (courseId != null) {
            wrapper.eq(StudyNote::getCourseId, courseId);
        }
        List<StudyNote> notes = noteMapper.selectList(wrapper);
        if (notes.isEmpty()) {
            return new ArrayList<>();
        }
        Map<Long, Course> courses = courseMapper
                .selectBatchIds(notes.stream().map(StudyNote::getCourseId).distinct().toList())
                .stream().collect(Collectors.toMap(Course::getId, c -> c));
        List<Long> chapterIds = notes.stream().map(StudyNote::getChapterId)
                .filter(Objects::nonNull).distinct().toList();
        Map<Long, String> chapterTitles = chapterIds.isEmpty()
                ? Map.of()
                : chapterMapper.selectBatchIds(chapterIds).stream()
                .collect(Collectors.toMap(Chapter::getId, Chapter::getTitle));

        List<Map<String, Object>> result = new ArrayList<>();
        for (StudyNote n : notes) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", n.getId());
            item.put("courseId", n.getCourseId());
            Course c = courses.get(n.getCourseId());
            item.put("courseTitle", c == null ? "" : c.getTitle());
            item.put("chapterId", n.getChapterId());
            item.put("chapterTitle", n.getChapterId() == null
                    ? "" : chapterTitles.getOrDefault(n.getChapterId(), ""));
            item.put("refType", n.getRefType());
            item.put("refId", n.getRefId());
            item.put("title", n.getTitle() == null ? "" : n.getTitle());
            item.put("content", n.getContent() == null ? "" : n.getContent());
            item.put("updateTime", n.getUpdateTime() == null ? "" : n.getUpdateTime().toString());
            result.add(item);
        }
        return result;
    }

    /** 新增或修改笔记，返回笔记 id（前端保存后可直接定位/刷新） */
    public Long saveNote(StudyNote note, Long userId) {
        String plain = plainText(note.getContent());
        if (plain.isBlank()) {
            throw new BusinessException("笔记内容不能为空");
        }
        if (note.getCourseId() == null) {
            throw new BusinessException("请选择所属课程");
        }
        if (note.getContent().length() > 200_000) {
            throw new BusinessException("笔记太长了，请精简后再保存");
        }
        if (note.getTitle() == null || note.getTitle().isBlank()) {
            note.setTitle(plain.length() > 20 ? plain.substring(0, 20) + "…" : plain);
        }
        note.setUserId(userId);
        note.setUpdateTime(LocalDateTime.now());
        if (note.getId() == null) {
            note.setCreateTime(LocalDateTime.now());
            noteMapper.insert(note);
        } else {
            StudyNote exists = noteMapper.selectById(note.getId());
            if (exists == null || !exists.getUserId().equals(userId)) {
                throw new BusinessException(403, "只能修改自己的笔记");
            }
            note.setCreateTime(exists.getCreateTime());
            noteMapper.updateById(note);
        }
        return note.getId();
    }

    /** 富文本转纯文本：去掉标签、图片与多余空白，用来生成笔记标题与判空 */
    public static String plainText(String html) {
        if (html == null) {
            return "";
        }
        return html.replaceAll("(?is)<img[^>]*>", " ")
                .replaceAll("(?is)<br\\s*/?>", "\n")
                .replaceAll("(?is)</(p|div|li|h[1-6])>", "\n")
                .replaceAll("(?s)<[^>]+>", "")
                .replace("&nbsp;", " ")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&amp;", "&")
                .replaceAll("[ \\t]+", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }

    public void removeNote(Long id, Long userId) {        StudyNote note = noteMapper.selectById(id);
        if (note == null) {
            throw new BusinessException("笔记不存在");
        }
        if (!note.getUserId().equals(userId)) {
            throw new BusinessException(403, "只能删除自己的笔记");
        }
        noteMapper.deleteById(id);
    }

    // ==================== 学习概览 ====================

    /**
     * 学员首页/个人中心的学习概览。
     * 学习时长没有单独打点表，这里用「已完成章节数」作为活跃度口径，
     * 前端文案要跟着说「完成章节」，不要写成「学习时长」。
     */
    public Map<String, Object> overview(Long userId) {
        List<Long> courseIds = enrollmentMapper.selectList(new LambdaQueryWrapper<Enrollment>()
                        .eq(Enrollment::getUserId, userId)
                        .eq(Enrollment::getStatus, 1))
                .stream().map(Enrollment::getCourseId).collect(Collectors.toList());

        long totalChapters = 0;
        for (Long cid : courseIds) {
            Long cnt = chapterMapper.selectCount(new LambdaQueryWrapper<Chapter>()
                    .eq(Chapter::getCourseId, cid));
            totalChapters += cnt == null ? 0 : cnt;
        }

        List<StudyProgress> progresses = progressMapper.selectList(new LambdaQueryWrapper<StudyProgress>()
                .eq(StudyProgress::getUserId, userId)
                .eq(StudyProgress::getFinished, 1));
        long finishedChapters = progresses.size();
        int completionRate = totalChapters == 0
                ? 0 : (int) Math.min(100, Math.round(finishedChapters * 100.0 / totalChapters));

        // 学习天数与连续打卡
        Set<LocalDate> days = progresses.stream()
                .map(StudyProgress::getFinishTime)
                .filter(Objects::nonNull)
                .map(LocalDateTime::toLocalDate)
                .collect(Collectors.toSet());
        int streak = 0;
        LocalDate cursor = LocalDate.now();
        // 今天没学不算断，从昨天继续往前找
        if (!days.contains(cursor)) {
            cursor = cursor.minusDays(1);
        }
        while (days.contains(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }

        // 最近 7 天每日完成章节数
        List<Map<String, Object>> week = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate d = LocalDate.now().minusDays(i);
            long count = progresses.stream()
                    .filter(p -> p.getFinishTime() != null && p.getFinishTime().toLocalDate().equals(d))
                    .count();
            Map<String, Object> bar = new LinkedHashMap<>();
            bar.put("date", d.format(MD));
            bar.put("label", WEEK_CN[d.getDayOfWeek().getValue() - 1]);
            bar.put("count", count);
            bar.put("isToday", i == 0);
            week.add(bar);
        }

        // 成绩趋势：作业 + 测验合并，按时间取最近 6 条
        record ScorePoint(String name, int score, String time, String type) {
        }
        List<ScorePoint> points = new ArrayList<>();
        submissionMapper.selectList(new LambdaQueryWrapper<Submission>()
                        .eq(Submission::getUserId, userId)
                        .isNotNull(Submission::getScore))
                .forEach(s -> points.add(new ScorePoint("作业", s.getScore(),
                        s.getSubmitTime() == null ? "" : s.getSubmitTime().toString(), "作业")));
        recordMapper.selectList(new LambdaQueryWrapper<ExamRecord>()
                        .eq(ExamRecord::getUserId, userId)
                        .isNotNull(ExamRecord::getScore))
                .forEach(r -> points.add(new ScorePoint("测验", r.getScore(),
                        r.getSubmitTime() == null ? "" : r.getSubmitTime().toString(), "测验")));
        List<Map<String, Object>> scoreTrend = points.stream()
                .sorted(Comparator.comparing(ScorePoint::time))
                .skip(Math.max(0, points.size() - 6))
                .map(p -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("name", p.type());
                    m.put("score", p.score());
                    m.put("time", p.time().length() >= 10 ? p.time().substring(5, 10) : p.time());
                    return m;
                })
                .collect(Collectors.toList());

        // 待办：未交且未过期的作业
        long todoAssignments = 0;
        for (Long cid : courseIds) {
            List<Assignment> list = assignmentMapper.selectList(new LambdaQueryWrapper<Assignment>()
                    .eq(Assignment::getCourseId, cid));
            for (Assignment a : list) {
                boolean submitted = submissionMapper.selectCount(new LambdaQueryWrapper<Submission>()
                        .eq(Submission::getAssignmentId, a.getId())
                        .eq(Submission::getUserId, userId)) > 0;
                boolean open = a.getDeadline() == null || a.getDeadline().isAfter(LocalDateTime.now());
                if (!submitted && open) {
                    todoAssignments++;
                }
            }
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("courseCount", courseIds.size());
        data.put("totalChapters", totalChapters);
        data.put("finishedChapters", finishedChapters);
        data.put("completionRate", completionRate);
        data.put("studyDays", days.size());
        data.put("streak", streak);
        data.put("week", week);
        data.put("weekTotal", week.stream().mapToLong(b -> (Long) b.get("count")).sum());
        data.put("scoreTrend", scoreTrend);
        data.put("todoAssignments", todoAssignments);
        data.put("favoriteCount", favoriteMapper.selectCount(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId)));
        data.put("noteCount", noteMapper.selectCount(new LambdaQueryWrapper<StudyNote>()
                .eq(StudyNote::getUserId, userId)));
        return data;
    }
}
