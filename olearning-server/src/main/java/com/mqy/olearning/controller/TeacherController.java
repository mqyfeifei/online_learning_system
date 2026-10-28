package com.mqy.olearning.controller;

import com.mqy.olearning.common.Result;
import com.mqy.olearning.dto.CourseDTO;
import com.mqy.olearning.dto.CourseVO;
import com.mqy.olearning.dto.ExamResultVO;
import com.mqy.olearning.dto.QuestionVO;
import com.mqy.olearning.entity.*;
import com.mqy.olearning.security.RequireRole;
import com.mqy.olearning.security.UserContext;
import com.mqy.olearning.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
    private final QuestionIOService questionIOService;

    /** 生成 xlsx 下载响应 */
    private ResponseEntity<byte[]> xlsxResponse(byte[] bytes, String fileName) {
        String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

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

    @Operation(summary = "题库导出为 xlsx（可直接改完再导回）")
    @GetMapping("/courses/{courseId}/questions/export")
    public ResponseEntity<byte[]> exportQuestions(@PathVariable Long courseId) {
        byte[] bytes = questionIOService.export(courseId, UserContext.userId());
        return xlsxResponse(bytes, "题库导出.xlsx");
    }

    @Operation(summary = "下载题库导入模板")
    @GetMapping("/questions/import-template")
    public ResponseEntity<byte[]> questionTemplate() {
        return xlsxResponse(questionIOService.template(), "题库导入模板.xlsx");
    }

    @Operation(summary = "从 xlsx 导入题目，逐行返回成功/失败明细")
    @PostMapping("/courses/{courseId}/questions/import")
    public Result<Map<String, Object>> importQuestions(@PathVariable Long courseId,
                                                       @RequestParam("file") MultipartFile file) {
        return Result.ok(questionIOService.importQuestions(courseId, UserContext.userId(), file));
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

    @Operation(summary = "发布试卷（重新编辑后需重新发布；要求题目分值合计 == 试卷总分）")
    @PostMapping("/papers/{id}/publish")
    public Result<Void> publishPaper(@PathVariable Long id) {
        examService.publishPaper(id, UserContext.userId());
        return Result.ok();
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

    @Operation(summary = "直接修改试卷里某题的分值：body.score")
    @PutMapping("/paper-questions/{id}/score")
    public Result<Void> updatePaperQuestionScore(@PathVariable Long id,
                                                 @RequestBody Map<String, Object> body) {
        Integer score = body.get("score") == null ? null : Integer.valueOf(body.get("score").toString());
        examService.updatePaperQuestionScore(id, score);
        return Result.ok();
    }

    @Operation(summary = "某份试卷的答卷列表（含待批阅状态）")
    @GetMapping("/papers/{id}/records")
    public Result<List<Map<String, Object>>> paperRecords(@PathVariable Long id) {
        return Result.ok(examService.paperRecords(id, UserContext.userId()));
    }

    @Operation(summary = "答卷详情（教师视角，含参考答案）")
    @GetMapping("/records/{id}")
    public Result<ExamResultVO> recordDetail(@PathVariable Long id) {
        return Result.ok(examService.teacherRecordDetail(id, UserContext.userId()));
    }

    @Operation(summary = "批阅主观题：body.marks = [{questionId, score, comment}]")
    @PostMapping("/records/{id}/mark")
    @SuppressWarnings("unchecked")
    public Result<Void> markRecord(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        List<Map<String, Object>> marks = (List<Map<String, Object>>) body.get("marks");
        examService.markRecord(id, marks, UserContext.userId());
        return Result.ok();
    }

    // ---------------- 学生与学情 ----------------

    @Operation(summary = "本课程学生名单与进度")
    @GetMapping("/courses/{courseId}/students")
    public Result<List<Map<String, Object>>> students(@PathVariable Long courseId) {
        return Result.ok(enrollmentService.students(courseId, UserContext.userId()));
    }

    @Operation(summary = "待审核的选课申请")
    @GetMapping("/courses/{courseId}/enroll-applies")
    public Result<List<Map<String, Object>>> enrollApplies(@PathVariable Long courseId) {
        return Result.ok(enrollmentService.pendingApplies(courseId, UserContext.userId()));
    }

    @Operation(summary = "审批选课申请：body.pass = true/false")
    @PostMapping("/enroll-applies/{id}/audit")
    public Result<Void> auditApply(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        boolean pass = Boolean.parseBoolean(String.valueOf(body.get("pass")));
        enrollmentService.auditApply(id, pass, UserContext.userId());
        return Result.ok();
    }

    @Operation(summary = "导入学生名单：body.text 每行一个登录名")
    @PostMapping("/courses/{courseId}/students/import")
    public Result<Map<String, Object>> importStudents(@PathVariable Long courseId,
                                                      @RequestBody Map<String, String> body) {
        return Result.ok(enrollmentService.importStudents(courseId, UserContext.userId(), body.get("text")));
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
