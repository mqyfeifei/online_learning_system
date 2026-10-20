package com.mqy.olearning.controller;

import com.mqy.olearning.common.PageResult;
import com.mqy.olearning.common.Result;
import com.mqy.olearning.dto.CourseVO;
import com.mqy.olearning.entity.CourseCategory;
import com.mqy.olearning.entity.SysNotice;
import com.mqy.olearning.entity.SysUser;
import com.mqy.olearning.security.RequireRole;
import com.mqy.olearning.security.UserContext;
import com.mqy.olearning.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 管理端：用户、课程审核、分类、公告、统计、讨论区内容审核
 */
@Tag(name = "管理端")
@RestController
@RequestMapping("/api/admin")
@RequireRole("ADMIN")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;
    private final CourseService courseService;
    private final CategoryService categoryService;
    private final NoticeService noticeService;
    private final StatsService statsService;
    private final DiscussionService discussionService;

    // ---------------- 用户管理 ----------------

    @Operation(summary = "用户列表")
    @GetMapping("/users")
    public Result<PageResult<SysUser>> users(@RequestParam(required = false) String keyword,
                                             @RequestParam(required = false) String role,
                                             @RequestParam(defaultValue = "1") long current,
                                             @RequestParam(defaultValue = "10") long size) {
        return Result.ok(userService.adminPage(keyword, role, current, size));
    }

    @Operation(summary = "启用/禁用（教师注册审核也走这里）")
    @PostMapping("/users/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        userService.updateStatus(id, status);
        return Result.ok();
    }

    @Operation(summary = "重置密码为 123456")
    @PostMapping("/users/{id}/reset")
    public Result<Void> resetPassword(@PathVariable Long id) {
        userService.resetPassword(id);
        return Result.ok();
    }

    @Operation(summary = "分配角色")
    @PostMapping("/users/{id}/role")
    public Result<Void> updateRole(@PathVariable Long id, @RequestParam String role) {
        userService.updateRole(id, role);
        return Result.ok();
    }

    // ---------------- 课程审核 ----------------

    @Operation(summary = "课程列表（默认查待审核 status=1）")
    @GetMapping("/courses")
    public Result<PageResult<CourseVO>> courses(@RequestParam(required = false) Integer status,
                                                @RequestParam(defaultValue = "1") long current,
                                                @RequestParam(defaultValue = "10") long size) {
        return Result.ok(courseService.auditPage(status, current, size));
    }

    @Operation(summary = "审核：pass=true 通过，false 驳回并记录理由")
    @PostMapping("/courses/{id}/audit")
    public Result<Void> audit(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        boolean pass = Boolean.parseBoolean(String.valueOf(body.get("pass")));
        String reason = body.get("reason") == null ? null : body.get("reason").toString();
        courseService.audit(id, pass, reason);
        return Result.ok();
    }

    // ---------------- 分类管理 ----------------

    @Operation(summary = "分类平铺列表")
    @GetMapping("/categories")
    public Result<List<CourseCategory>> categories() {
        return Result.ok(categoryService.list());
    }

    @Operation(summary = "新增/编辑分类")
    @PostMapping("/categories")
    public Result<Void> saveCategory(@RequestBody CourseCategory category) {
        categoryService.save(category);
        return Result.ok();
    }

    @Operation(summary = "删除分类")
    @DeleteMapping("/categories/{id}")
    public Result<Void> deleteCategory(@PathVariable Long id) {
        categoryService.remove(id);
        return Result.ok();
    }

    // ---------------- 公告管理 ----------------

    @Operation(summary = "全部公告")
    @GetMapping("/notices")
    public Result<List<SysNotice>> notices() {
        return Result.ok(noticeService.adminList());
    }

    @Operation(summary = "发布/修改公告")
    @PostMapping("/notices")
    public Result<Void> saveNotice(@RequestBody SysNotice notice) {
        noticeService.save(notice);
        return Result.ok();
    }

    @Operation(summary = "显示/撤下公告")
    @PostMapping("/notices/{id}/status")
    public Result<Void> noticeStatus(@PathVariable Long id, @RequestParam Integer status) {
        noticeService.updateStatus(id, status);
        return Result.ok();
    }

    @Operation(summary = "删除公告")
    @DeleteMapping("/notices/{id}")
    public Result<Void> deleteNotice(@PathVariable Long id) {
        noticeService.remove(id);
        return Result.ok();
    }

    // ---------------- 内容审核与统计 ----------------

    @Operation(summary = "删除讨论帖（内容审核）")
    @DeleteMapping("/discussions/{id}")
    public Result<Void> deletePost(@PathVariable Long id) {
        discussionService.remove(id, UserContext.userId(), "ADMIN");
        return Result.ok();
    }

    @Operation(summary = "平台统计")
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        return Result.ok(statsService.platform());
    }
}
