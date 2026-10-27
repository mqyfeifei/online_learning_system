package com.mqy.olearning.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mqy.olearning.entity.ExamPaper;
import com.mqy.olearning.entity.ExamRecord;
import com.mqy.olearning.mapper.ExamPaperMapper;
import com.mqy.olearning.mapper.ExamRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 课程「作业与测验」聚合：课程详情页的「作业测试」页签用同一份数据，
 * 统一给出状态（未开始 / 进行中 / 已完成 / 已截止），前端不再各自拼逻辑。
 */
@Service
@RequiredArgsConstructor
public class CourseWorkService {

    private static final DateTimeFormatter MINUTE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final AssignmentService assignmentService;
    private final ExamPaperMapper paperMapper;
    private final ExamRecordMapper recordMapper;

    public Map<String, Object> works(Long courseId, Long userId) {
        List<Map<String, Object>> assignments = assignmentService.listByCourse(courseId, userId);

        List<ExamPaper> papers = paperMapper.selectList(new LambdaQueryWrapper<ExamPaper>()
                .eq(ExamPaper::getCourseId, courseId)
                .eq(ExamPaper::getStatus, 1)
                .orderByDesc(ExamPaper::getId));

        List<Map<String, Object>> exams = new ArrayList<>();
        int done = 0;
        for (ExamPaper p : papers) {
            ExamRecord record = null;
            if (userId != null) {
                List<ExamRecord> records = recordMapper.selectList(new LambdaQueryWrapper<ExamRecord>()
                        .eq(ExamRecord::getPaperId, p.getId())
                        .eq(ExamRecord::getUserId, userId)
                        .orderByDesc(ExamRecord::getId));
                record = records.isEmpty() ? null : records.get(0);
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("paper", p);
            item.put("workType", "EXAM");
            item.put("record", record);
            item.put("state", examState(p, record));
            item.put("startText", p.getStartTime() == null ? "" : MINUTE.format(p.getStartTime()));
            exams.add(item);
        }

        for (Map<String, Object> a : assignments) {
            String state = String.valueOf(a.get("state"));
            if ("SUBMITTED".equals(state) || "GRADED".equals(state)) {
                done++;
            }
        }
        for (Map<String, Object> e : exams) {
            if ("SUBMITTED".equals(e.get("state")) || "GRADED".equals(e.get("state"))) {
                done++;
            }
        }

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total", assignments.size() + exams.size());
        stats.put("done", done);
        stats.put("assignmentCount", assignments.size());
        stats.put("examCount", exams.size());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("assignments", assignments);
        data.put("exams", exams);
        data.put("stats", stats);
        return data;
    }

    private String examState(ExamPaper paper, ExamRecord record) {
        if (record != null) {
            return record.getMarkStatus() != null && record.getMarkStatus() == 2 ? "GRADED" : "SUBMITTED";
        }
        LocalDateTime now = LocalDateTime.now();
        if (paper.getStartTime() != null && now.isBefore(paper.getStartTime())) {
            return "NOT_START";
        }
        if (paper.getEndTime() != null && now.isAfter(paper.getEndTime())) {
            return "EXPIRED";
        }
        return "ONGOING";
    }
}
