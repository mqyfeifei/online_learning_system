package com.mqy.olearning.controller;

import com.mqy.olearning.common.Result;
import com.mqy.olearning.dto.CourseVO;
import com.mqy.olearning.entity.StudyNote;
import com.mqy.olearning.security.UserContext;
import com.mqy.olearning.service.StudyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 学习增强：收藏、笔记、学习概览
 * 注意路径都在 /api/my/** 或 /api/courses/{id}/favorite 下，
 * 归入 JWT 拦截器保护范围，任何已登录角色都能用
 */
@Tag(name = "学习增强")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class StudyController {

    private final StudyService studyService;

    // ---------------- 收藏 ----------------

    @Operation(summary = "收藏/取消收藏课程")
    @PostMapping("/courses/{courseId}/favorite")
    public Result<Map<String, Object>> toggleFavorite(@PathVariable Long courseId) {
        boolean favored = studyService.toggleFavorite(UserContext.userId(), courseId);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("favored", favored);
        return Result.ok(data);
    }

    @Operation(summary = "查询是否已收藏")
    @GetMapping("/courses/{courseId}/favorite")
    public Result<Map<String, Object>> favoriteState(@PathVariable Long courseId) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("favored", studyService.isFavorite(UserContext.userId(), courseId));
        return Result.ok(data);
    }

    @Operation(summary = "我的收藏课程")
    @GetMapping("/my/favorites")
    public Result<List<CourseVO>> myFavorites() {
        return Result.ok(studyService.myFavorites(UserContext.userId()));
    }

    // ---------------- 笔记 ----------------

    @Operation(summary = "我的笔记（可按课程过滤）")
    @GetMapping("/my/notes")
    public Result<List<Map<String, Object>>> myNotes(@RequestParam(required = false) Long courseId) {
        return Result.ok(studyService.myNotes(UserContext.userId(), courseId));
    }

    @Operation(summary = "笔记按课程分文件夹：一门课一个文件夹")
    @GetMapping("/my/notes/groups")
    public Result<List<Map<String, Object>>> noteGroups() {
        return Result.ok(studyService.noteGroups(UserContext.userId()));
    }

    @Operation(summary = "新增或修改笔记，返回笔记 id")
    @PostMapping("/my/notes")
    public Result<Long> saveNote(@RequestBody StudyNote note) {
        return Result.ok(studyService.saveNote(note, UserContext.userId()));
    }

    @Operation(summary = "删除笔记")
    @DeleteMapping("/my/notes/{id}")
    public Result<Void> removeNote(@PathVariable Long id) {
        studyService.removeNote(id, UserContext.userId());
        return Result.ok();
    }

    // ---------------- 学习概览 ----------------

    @Operation(summary = "学习概览：进度、连续打卡、周活跃、成绩趋势")
    @GetMapping("/my/overview")
    public Result<Map<String, Object>> overview() {
        return Result.ok(studyService.overview(UserContext.userId()));
    }
}
