package com.mqy.olearning.controller;

import com.mqy.olearning.security.UserContext;
import com.mqy.olearning.service.ExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 作业 / 试卷导出为 docx。
 * <p>权限由课程的 exportScope 控制，教师与管理员不受限制。
 */
@Tag(name = "导出")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ExportController {

    private final ExportService exportService;

    @Operation(summary = "导出作业（含提交名单）为 docx")
    @GetMapping("/courses/{courseId}/assignments/{assignmentId}/export")
    public ResponseEntity<byte[]> exportAssignment(@PathVariable Long courseId,
                                                   @PathVariable Long assignmentId) {
        byte[] bytes = exportService.exportAssignment(courseId, assignmentId,
                UserContext.userId(), UserContext.role());
        return docx(bytes, "作业导出.docx");
    }

    @Operation(summary = "导出试卷为 docx，withAnswer=true 时附参考答案（教师用）")
    @GetMapping("/courses/{courseId}/papers/{paperId}/export")
    public ResponseEntity<byte[]> exportPaper(@PathVariable Long courseId,
                                              @PathVariable Long paperId,
                                              @RequestParam(defaultValue = "false") boolean withAnswer) {
        byte[] bytes = exportService.exportPaper(courseId, paperId,
                UserContext.userId(), UserContext.role(), withAnswer);
        return docx(bytes, "试卷导出.docx");
    }

    private ResponseEntity<byte[]> docx(byte[] bytes, String fileName) {
        String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .body(bytes);
    }
}
