package com.mqy.olearning.controller;

import com.mqy.olearning.common.PageResult;
import com.mqy.olearning.common.Result;
import com.mqy.olearning.dto.CourseVO;
import com.mqy.olearning.entity.CourseCategory;
import com.mqy.olearning.entity.CourseResource;
import com.mqy.olearning.entity.SysNotice;
import com.mqy.olearning.security.UserContext;
import com.mqy.olearning.service.CategoryService;
import com.mqy.olearning.service.ChapterService;
import com.mqy.olearning.service.CourseService;
import com.mqy.olearning.service.CourseWorkService;
import com.mqy.olearning.service.DiscussionService;
import com.mqy.olearning.service.NoticeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 前台公开接口：未登录也能浏览课程广场、公告、讨论区
 * 登录后 UserContext 有值，会额外返回选课状态与进度
 */
@Tag(name = "前台公开接口")
@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicController {

    private final CourseService courseService;
    private final CategoryService categoryService;
    private final NoticeService noticeService;
    private final DiscussionService discussionService;
    private final ChapterService chapterService;
    private final CourseWorkService courseWorkService;

    @Operation(summary = "课程广场：分类筛选 + 关键词 + 热度/最新排序")
    @GetMapping("/courses")
    public Result<PageResult<CourseVO>> courses(@RequestParam(required = false) Long categoryId,
                                                @RequestParam(required = false) String keyword,
                                                @RequestParam(defaultValue = "new") String sort,
                                                @RequestParam(defaultValue = "1") long current,
                                                @RequestParam(defaultValue = "9") long size) {
        return Result.ok(courseService.publicPage(categoryId, keyword, sort, current, size, UserContext.userId()));
    }

    @Operation(summary = "课程详情：含章节目录与当前用户进度")
    @GetMapping("/courses/{id}")
    public Result<Map<String, Object>> courseDetail(@PathVariable Long id) {
        return Result.ok(courseService.detail(id, UserContext.userId()));
    }

    @Operation(summary = "课程课件资料（游客可浏览，下载不需要选课）")
    @GetMapping("/courses/{id}/resources")
    public Result<List<CourseResource>> courseResources(@PathVariable Long id) {
        return Result.ok(chapterService.resources(id, null));
    }

    @Operation(summary = "课程作业与测验汇总（含我的完成状态）")
    @GetMapping("/courses/{id}/works")
    public Result<Map<String, Object>> courseWorks(@PathVariable Long id) {
        return Result.ok(courseWorkService.works(id, UserContext.userId()));
    }

    @Operation(summary = "分类树")
    @GetMapping("/categories")
    public Result<List<Map<String, Object>>> categories() {
        return Result.ok(categoryService.tree());
    }

    @Operation(summary = "分类平铺列表")
    @GetMapping("/categories/flat")
    public Result<List<CourseCategory>> categoriesFlat() {
        return Result.ok(categoryService.list());
    }

    @Operation(summary = "公告列表")
    @GetMapping("/notices")
    public Result<List<SysNotice>> notices() {
        return Result.ok(noticeService.list());
    }

    @Operation(summary = "讨论帖列表")
    @GetMapping("/discussions")
    public Result<PageResult<Map<String, Object>>> discussions(@RequestParam(required = false) Long courseId,
                                                              @RequestParam(required = false) String keyword,
                                                              @RequestParam(defaultValue = "1") long current,
                                                              @RequestParam(defaultValue = "10") long size) {
        return Result.ok(discussionService.posts(courseId, keyword, current, size));
    }

    @Operation(summary = "帖子详情 + 回复列表")
    @GetMapping("/discussions/{id}")
    public Result<Map<String, Object>> discussionDetail(@PathVariable Long id) {
        return Result.ok(discussionService.detail(id));
    }
}
