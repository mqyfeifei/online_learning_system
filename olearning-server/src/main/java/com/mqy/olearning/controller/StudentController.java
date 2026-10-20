package com.mqy.olearning.controller;

import com.mqy.olearning.common.Result;
import com.mqy.olearning.dto.ExamResultVO;
import com.mqy.olearning.dto.ExamSubmitDTO;
import com.mqy.olearning.entity.*;
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
 * 学生端：个人信息、选课、学习打卡、作业、测验、成绩、讨论
 */
@Tag(name = "学生端")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class StudentController {

    private final UserService userService;
    private final EnrollmentService enrollmentService;
    private final ChapterService chapterService;
    private final AssignmentService assignmentService;
    private final ExamService examService;
    private final DiscussionService discussionService;
    private final StatsService statsService;

    // ---------------- 个人信息 ----------------

    @Operation(summary = "我的信息")
    @GetMapping("/user/profile")
    public Result<SysUser> profile() {
        return Result.ok(userService.getById(UserContext.userId()));
    }

    @Operation(summary = "修改昵称/手机号/头像")
    @PutMapping("/user/profile")
    public Result<Void> updateProfile(@RequestBody SysUser dto) {
        userService.updateProfile(UserContext.userId(), dto);
        return Result.ok();
    }

    @Operation(summary = "修改密码")
    @PutMapping("/user/password")
    public Result<Void> changePassword(@RequestBody Map<String, String> body) {
        userService.changePassword(UserContext.userId(),
                body.get("oldPassword"), body.get("newPassword"));
        return Result.ok();
    }

    // ---------------- 选课与学习 ----------------

    @Operation(summary = "选课")
    @PostMapping("/courses/{courseId}/enroll")
    public Result<Void> enroll(@PathVariable Long courseId) {
        enrollmentService.enroll(UserContext.userId(), courseId);
        return Result.ok();
    }

    @Operation(summary = "退课")
    @DeleteMapping("/courses/{courseId}/enroll")
    public Result<Void> cancelEnroll(@PathVariable Long courseId) {
        enrollmentService.cancel(UserContext.userId(), courseId);
        return Result.ok();
    }

    @Operation(summary = "我的课程（含学习进度）")
    @GetMapping("/my/courses")
    public Result<List<com.mqy.olearning.dto.CourseVO>> myCourses() {
        return Result.ok(enrollmentService.myCourses(UserContext.userId()));
    }

    @Operation(summary = "课程章节列表")
    @GetMapping("/courses/{courseId}/chapters")
    public Result<List<Chapter>> chapters(@PathVariable Long courseId) {
        return Result.ok(chapterService.listByCourse(courseId));
    }

    @Operation(summary = "学完本章打卡")
    @PostMapping("/chapters/{chapterId}/finish")
    public Result<Void> finishChapter(@PathVariable Long chapterId, @RequestParam Long courseId) {
        chapterService.finishChapter(UserContext.userId(), courseId, chapterId);
        return Result.ok();
    }

    @Operation(summary = "课程课件资源")
    @GetMapping("/courses/{courseId}/resources")
    public Result<List<CourseResource>> resources(@PathVariable Long courseId,
                                                  @RequestParam(required = false) Long chapterId) {
        return Result.ok(chapterService.resources(courseId, chapterId));
    }

    // ---------------- 作业 ----------------

    @Operation(summary = "课程作业列表（含我的提交）")
    @GetMapping("/courses/{courseId}/assignments")
    public Result<List<Map<String, Object>>> assignments(@PathVariable Long courseId) {
        return Result.ok(assignmentService.listByCourse(courseId, UserContext.userId()));
    }

    @Operation(summary = "提交作业")
    @PostMapping("/assignments/{assignmentId}/submit")
    public Result<Void> submitAssignment(@PathVariable Long assignmentId,
                                         @RequestBody Map<String, String> body) {
        assignmentService.submit(UserContext.userId(), assignmentId,
                body.get("content"), body.get("attachUrl"));
        return Result.ok();
    }

    // ---------------- 测验 ----------------

    @Operation(summary = "课程可参加的试卷列表")
    @GetMapping("/courses/{courseId}/papers")
    public Result<List<ExamPaper>> papers(@PathVariable Long courseId) {
        return Result.ok(examService.papers(courseId));
    }

    @Operation(summary = "开始答题（不含答案）")
    @GetMapping("/papers/{paperId}/start")
    public Result<Map<String, Object>> startExam(@PathVariable Long paperId) {
        return Result.ok(examService.startExam(paperId, UserContext.userId()));
    }

    @Operation(summary = "交卷判分")
    @PostMapping("/exams/submit")
    public Result<ExamResultVO> submitExam(@Valid @RequestBody ExamSubmitDTO dto) {
        return Result.ok(examService.submit(UserContext.userId(), dto));
    }

    @Operation(summary = "我的考试记录")
    @GetMapping("/my/records")
    public Result<List<Map<String, Object>>> myRecords() {
        return Result.ok(examService.myRecords(UserContext.userId()));
    }

    @Operation(summary = "答卷详情与错题解析")
    @GetMapping("/records/{recordId}")
    public Result<ExamResultVO> recordDetail(@PathVariable Long recordId) {
        return Result.ok(examService.recordDetail(recordId, UserContext.userId()));
    }

    @Operation(summary = "我的成绩汇总")
    @GetMapping("/my/grades")
    public Result<Map<String, Object>> myGrades() {
        Map<String, Object> stats = statsService.student(UserContext.userId());
        return Result.ok(stats);
    }

    // ---------------- 讨论区 ----------------

    @Operation(summary = "发帖")
    @PostMapping("/discussions")
    public Result<Long> createPost(@RequestBody DiscussionPost post) {
        return Result.ok(discussionService.createPost(post, UserContext.userId()));
    }

    @Operation(summary = "回复帖子")
    @PostMapping("/discussions/{postId}/reply")
    public Result<Void> reply(@PathVariable Long postId, @RequestBody Map<String, String> body) {
        discussionService.reply(postId, UserContext.userId(), body.get("content"));
        return Result.ok();
    }

    @Operation(summary = "点赞")
    @PostMapping("/discussions/{postId}/like")
    public Result<Void> like(@PathVariable Long postId) {
        discussionService.like(postId);
        return Result.ok();
    }

    @Operation(summary = "删除自己的帖子")
    @DeleteMapping("/discussions/{postId}")
    public Result<Void> deletePost(@PathVariable Long postId) {
        discussionService.remove(postId, UserContext.userId(), UserContext.role());
        return Result.ok();
    }
}
