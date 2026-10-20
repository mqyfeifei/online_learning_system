package com.mqy.olearning.controller;

import com.mqy.olearning.common.Result;
import com.mqy.olearning.dto.CourseDTO;
import com.mqy.olearning.dto.CourseVO;
import com.mqy.olearning.dto.QuestionVO;
import com.mqy.olearning.entity.*;
import com.mqy.olearning.security.RequireRole;
import com.mqy.olearning.security.UserContext;
import com.mqy.olearning.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 教师端：课程、章节、课件、作业批阅、题库、组卷、学情
 * 类上的 @RequireRole("TEACHER") 是「学生越权调教师接口返回 403」的实现点
 */
@Tag(name = "教师端")
@RestController
@RequestMapping("/api/teacher")
@RequireRole("TEACHER")
@RequiredArgsConstructor
public class TeacherController {

    private final CourseService courseService;
    private final ChapterService chapterService;
    private final AssignmentService assignmentService;
    private final ExamService examService;
    private final EnrollmentService enrollmentService;
    private final StatsService statsService;
    private final CategoryService categoryService;

    // ---------------- 课程 ----------------

    @Operation(summary = "我的课程")
    @GetMapping("/courses")
    public Result<List<CourseVO>> myCourses(@RequestParam(required = false) Integer status) {
        return Result.ok(courseService.teacherCourses(UserContext.userId(), status));
    }

    @Operation(summary = "新增/编辑课程")
    @PostMapping("/courses")
    public Result<Long> saveCourse(@Valid @RequestBody CourseDTO dto) {
        return Result.ok(courseService.save(dto, UserContext.userId()));
    }

    @Operation(summary = "提交审核")
    @PostMapping("/courses/{id}/submit")
    public Result<Void> submitAudit(@PathVariable Long id) {
        courseService.submitAudit(id, UserContext.userId());
        return Result.ok();
    }

    @Operation(summary = "下架课程")
    @PostMapping("/courses/{id}/offline")
    public Result<Void> offline(@PathVariable Long id) {
        courseService.offline(id, UserContext.userId());
        return Result.ok();
    }

    @Operation(summary = "删除课程（已发布需先下架）")
    @DeleteMapping("/courses/{id}")
    public Result<Void> deleteCourse(@PathVariable Long id) {
        courseService.remove(id, UserContext.userId());
        return Result.ok();
    }

    // ---------------- 章节 ----------------

    @Operation(summary = "章节列表")
    @GetMapping("/courses/{courseId}/chapters")
    public Result<List<Chapter>> chapters(@PathVariable Long courseId) {
        return Result.ok(chapterService.listByCourse(courseId));
    }

    @Operation(summary = "新增/编辑章节")
    @PostMapping("/chapters")
    public Result<Void> saveChapter(@RequestBody Chapter chapter) {
        chapterService.save(chapter, UserContext.userId());
        return Result.ok();
    }

    @Operation(summary = "删除章节")
    @DeleteMapping("/chapters/{id}")
    public Result<Void> deleteChapter(@PathVariable Long id) {
        chapterService.remove(id, UserContext.userId());
        return Result.ok();
    }

    // ---------------- 课件资源 ----------------

    @Operation(summary = "课件列表")
    @GetMapping("/courses/{courseId}/resources")
    public Result<List<CourseResource>> resources(@PathVariable Long courseId,
                                                  @RequestParam(required = false) Long chapterId) {
        return Result.ok(chapterService.resources(courseId, chapterId));
    }

    @Operation(summary = "添加课件记录（先调 /api/files/upload 拿 url）")
    @PostMapping("/resources")
    public Result<Void> saveResource(@RequestBody CourseResource resource) {
        chapterService.saveResource(resource);
        return Result.ok();
    }

    @Operation(summary = "删除课件")
    @DeleteMapping("/resources/{id}")
    public Result<Void> deleteResource(@PathVariable Long id) {
        chapterService.removeResource(id, UserContext.userId());
        return Result.ok();
    }

    // ---------------- 作业 ----------------

    @Operation(summary = "课程作业列表")
    @GetMapping("/courses/{courseId}/assignments")
    public Result<List<Map<String, Object>>> assignments(@PathVariable Long courseId) {
        return Result.ok(assignmentService.listByCourse(courseId, null));
    }

    @Operation(summary = "布置/修改作业")
    @PostMapping("/assignments")
    public Result<Void> saveAssignment(@RequestBody Assignment assignment) {
        assignmentService.save(assignment, UserContext.userId());
        return Result.ok();
    }

    @Operation(summary = "删除作业")
    @DeleteMapping("/assignments/{id}")
    public Result<Void> deleteAssignment(@PathVariable Long id) {
        assignmentService.remove(id, UserContext.userId());
        return Result.ok();
    }

    @Operation(summary = "作业提交列表")
    @GetMapping("/assignments/{id}/submissions")
    public Result<List<Map<String, Object>>> submissions(@PathVariable Long id) {
        return Result.ok(assignmentService.submissions(id, UserContext.userId()));
    }

    @Operation(summary = "批阅打分")
    @PostMapping("/submissions/{id}/grade")
    public Result<Void> grade(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Integer score = body.get("score") == null ? null : Integer.valueOf(body.get("score").toString());
        String comment = body.get("comment") == null ? null : body.get("comment").toString();
        assignmentService.grade(id, score, comment, UserContext.userId());
        return Result.ok();
    }

    // ---------------- 题库 ----------------

    @Operation(summary = "课程题库")
    @GetMapping("/courses/{courseId}/questions")
    public Result<List<Question>> questions(@PathVariable Long courseId) {
        return Result.ok(examService.questions(courseId));
    }

    @Operation(summary = "新增/编辑题目")
    @PostMapping("/questions")
    public Result<Void> saveQuestion(@RequestBody Question question) {
        examService.saveQuestion(question, UserContext.userId());
        return Result.ok();
    }

    @Operation(summary = "删除题目")
    @DeleteMapping("/questions/{id}")
    public Result<Void> deleteQuestion(@PathVariable Long id) {
        examService.removeQuestion(id, UserContext.userId());
        return Result.ok();
    }

    // ---------------- 试卷 ----------------

    @Operation(summary = "课程试卷列表")
    @GetMapping("/courses/{courseId}/papers")
    public Result<List<ExamPaper>> papers(@PathVariable Long courseId) {
        return Result.ok(examService.papers(courseId));
    }

    @Operation(summary = "创建/修改试卷")
    @PostMapping("/papers")
    public Result<Long> savePaper(@RequestBody ExamPaper paper) {
        return Result.ok(examService.savePaper(paper, UserContext.userId()));
    }

    @Operation(summary = "删除试卷")
    @DeleteMapping("/papers/{id}")
    public Result<Void> deletePaper(@PathVariable Long id) {
        examService.removePaper(id, UserContext.userId());
        return Result.ok();
    }

    @Operation(summary = "试卷题目（含答案）")
    @GetMapping("/papers/{id}/questions")
    public Result<List<QuestionVO>> paperQuestions(@PathVariable Long id) {
        return Result.ok(examService.paperQuestions(id, true));
    }

    @Operation(summary = "往试卷加题")
    @PostMapping("/papers/{id}/questions")
    public Result<Void> addPaperQuestion(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Long questionId = Long.valueOf(body.get("questionId").toString());
        Integer score = body.get("score") == null ? 10 : Integer.valueOf(body.get("score").toString());
        examService.addQuestion(id, questionId, score);
        return Result.ok();
    }

    @Operation(summary = "从试卷移除题目")
    @DeleteMapping("/paper-questions/{id}")
    public Result<Void> removePaperQuestion(@PathVariable Long id) {
        examService.removePaperQuestion(id);
        return Result.ok();
    }

    // ---------------- 学生与学情 ----------------

    @Operation(summary = "本课程学生名单与进度")
    @GetMapping("/courses/{courseId}/students")
    public Result<List<Map<String, Object>>> students(@PathVariable Long courseId) {
        return Result.ok(enrollmentService.students(courseId, UserContext.userId()));
    }

    @Operation(summary = "本课程考试记录")
    @GetMapping("/courses/{courseId}/records")
    public Result<List<Map<String, Object>>> records(@PathVariable Long courseId) {
        return Result.ok(examService.courseRecords(courseId, UserContext.userId()));
    }

    @Operation(summary = "学情看板数据（ECharts）")
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        return Result.ok(statsService.teacher(UserContext.userId()));
    }

    @Operation(summary = "分类列表（建课时选分类）")
    @GetMapping("/categories")
    public Result<List<CourseCategory>> categories() {
        return Result.ok(categoryService.list());
    }
}
