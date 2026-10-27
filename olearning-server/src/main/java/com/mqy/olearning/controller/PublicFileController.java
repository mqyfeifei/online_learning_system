package com.mqy.olearning.controller;

import com.mqy.olearning.common.BusinessException;
import com.mqy.olearning.common.Result;
import com.mqy.olearning.entity.Course;
import com.mqy.olearning.entity.CourseResource;
import com.mqy.olearning.mapper.CourseMapper;
import com.mqy.olearning.mapper.CourseResourceMapper;
import com.mqy.olearning.service.DocPreviewService;
import com.mqy.olearning.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 文件访问：在线预览、原文件流、下载。
 * <p>两种寻址方式：
 * <ul>
 *   <li>{@code rid}：课件资源 id，下载受课程「资料是否可下载」开关控制；</li>
 *   <li>{@code url}：任意已上传文件的访问地址（作业附件、提交附件、头像、封面、笔记图片等），
 *       只要在本站 upload 目录内就能直接预览，不受课件开关限制。</li>
 * </ul>
 * 放在 /api/public 下是为了让图片、PDF 这类资源能在 &lt;img&gt; / &lt;iframe&gt; 里直接引用。
 */
@Slf4j
@Tag(name = "文件预览")
@RestController
@RequestMapping("/api/public/files")
@RequiredArgsConstructor
public class PublicFileController {

    private static final Set<String> IMAGES = Set.of("png", "jpg", "jpeg", "gif", "bmp", "webp", "svg");
    private static final Set<String> VIDEOS = Set.of("mp4", "webm", "mov", "m4v", "ogv");
    private static final Set<String> AUDIOS = Set.of("mp3", "wav", "m4a", "aac", "flac");
    private static final Set<String> TEXTS = Set.of("txt", "md", "csv", "json", "log", "java", "sql",
            "html", "css", "js", "ts", "xml", "yml", "yaml", "properties");
    private static final Set<String> OFFICE = Set.of("docx", "doc", "pptx", "ppt", "xlsx", "xls");

    private final CourseResourceMapper resourceMapper;
    private final CourseMapper courseMapper;
    private final FileService fileService;
    private final DocPreviewService docPreviewService;

    @Operation(summary = "在线预览：按类型返回可直接渲染的结构（rid 或 url 二选一）")
    @GetMapping("/preview")
    public Result<Map<String, Object>> preview(@RequestParam(required = false) Long rid,
                                               @RequestParam(required = false) String url,
                                               @RequestParam(required = false) String name) {
        Target target = resolveTarget(rid, url, name);
        File file = fileService.resolve(target.fileUrl());
        String ext = fileService.extOf(target.name());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("name", target.name());
        data.put("fileType", ext);
        data.put("fileSize", file.length());
        data.put("downloadable", target.downloadable());
        data.put("rawUrl", target.rawUrl());
        data.put("downloadUrl", target.downloadUrl());

        if ("pdf".equals(ext)) {
            data.put("kind", "pdf");
        } else if (IMAGES.contains(ext)) {
            data.put("kind", "image");
        } else if (VIDEOS.contains(ext)) {
            data.put("kind", "video");
        } else if (AUDIOS.contains(ext)) {
            data.put("kind", "audio");
        } else if (TEXTS.contains(ext)) {
            data.put("kind", "text");
            data.put("text", readText(file));
        } else if (OFFICE.contains(ext)) {
            data.putAll(docPreviewService.preview(file, ext));
            // 解析结果里的 name 用文件名，这里改回原始显示名
            data.put("name", target.name());
        } else {
            data.put("kind", "unsupported");
            data.put("message", "该格式暂不支持在线预览，可下载后查看");
        }
        return Result.ok(data);
    }

    @Operation(summary = "原文件流（PDF/图片/音视频在浏览器内直接打开）")
    @GetMapping("/raw")
    public ResponseEntity<Resource> raw(@RequestParam(required = false) Long rid,
                                        @RequestParam(required = false) String url,
                                        @RequestParam(required = false) String name) {
        Target target = resolveTarget(rid, url, name);
        File file = fileService.resolve(target.fileUrl());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename*=UTF-8''" + encode(target.name()))
                .contentType(mediaType(target.name()))
                .body(new FileSystemResource(file));
    }

    @Operation(summary = "下载文件（课件会校验课程开关，其它附件直接放行）")
    @GetMapping("/download")
    public ResponseEntity<Resource> download(@RequestParam(required = false) Long rid,
                                             @RequestParam(required = false) String url,
                                             @RequestParam(required = false) String name) {
        Target target = resolveTarget(rid, url, name);
        if (!target.downloadable()) {
            throw new BusinessException(403, "本课程资料仅支持在线查看，未开放下载");
        }
        File file = fileService.resolve(target.fileUrl());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encode(target.name()))
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new FileSystemResource(file));
    }

    // ---------------- 工具 ----------------

    /** 统一两种寻址方式，后续逻辑只认「文件地址 + 展示名 + 是否可下载」 */
    private record Target(String fileUrl, String name, boolean downloadable,
                          String rawUrl, String downloadUrl) {
    }

    private Target resolveTarget(Long rid, String url, String displayName) {
        if (rid != null) {
            CourseResource resource = resourceMapper.selectById(rid);
            if (resource == null) {
                throw new BusinessException("课件不存在或已被删除");
            }
            return new Target(resource.getFileUrl(), resource.getName(), downloadable(resource),
                    "/api/public/files/raw?rid=" + rid,
                    "/api/public/files/download?rid=" + rid);
        }
        if (url == null || url.isBlank()) {
            throw new BusinessException("缺少文件标识");
        }
        String encoded = URLEncoder.encode(url.trim(), StandardCharsets.UTF_8);
        String shown = displayName != null && !displayName.isBlank() ? displayName : nameOf(url);
        String nameQuery = displayName != null && !displayName.isBlank()
                ? "&name=" + URLEncoder.encode(displayName, StandardCharsets.UTF_8) : "";
        return new Target(url.trim(), shown, true,
                "/api/public/files/raw?url=" + encoded + nameQuery,
                "/api/public/files/download?url=" + encoded + nameQuery);
    }

    /** 从 /files/2026/10/xxxx.pdf 里取出原始文件名（落盘时已加随机前缀，这里去掉） */
    private String nameOf(String url) {
        String path = url.replace("\\", "/");
        int slash = path.lastIndexOf('/');
        String fileName = slash < 0 ? path : path.substring(slash + 1);
        int query = fileName.indexOf('?');
        if (query > -1) {
            fileName = fileName.substring(0, query);
        }
        // 去掉 32 位 uuid 前缀，保留扩展名，展示更友好
        if (fileName.length() > 33 && fileName.charAt(32) == '.') {
            return "文件" + fileName.substring(32);
        }
        return fileName;
    }

    /** 课程级开关：0 仅在线查看，其它（含历史数据 null）视为允许下载 */
    private boolean downloadable(CourseResource resource) {
        Course course = courseMapper.selectById(resource.getCourseId());
        return course == null || course.getResourceDownloadable() == null
                || course.getResourceDownloadable() == 1;
    }

    private String readText(File file) {
        try {
            byte[] bytes = Files.readAllBytes(file.toPath());
            int limit = Math.min(bytes.length, 300 * 1024);
            return new String(bytes, 0, limit, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("文本读取失败: {}", file.getName(), e);
            return "（文件内容读取失败，请下载后查看）";
        }
    }

    private String encode(String name) {
        return URLEncoder.encode(name == null ? "file" : name, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private MediaType mediaType(String name) {
        String ext = fileService.extOf(name);
        return switch (ext) {
            case "pdf" -> MediaType.APPLICATION_PDF;
            case "png" -> MediaType.IMAGE_PNG;
            case "jpg", "jpeg" -> MediaType.IMAGE_JPEG;
            case "gif" -> MediaType.IMAGE_GIF;
            case "webp" -> MediaType.parseMediaType("image/webp");
            case "svg" -> MediaType.parseMediaType("image/svg+xml");
            case "mp4" -> MediaType.parseMediaType("video/mp4");
            case "webm" -> MediaType.parseMediaType("video/webm");
            case "mp3" -> MediaType.parseMediaType("audio/mpeg");
            case "txt", "md", "csv", "log", "java", "sql" -> MediaType.parseMediaType("text/plain;charset=UTF-8");
            default -> MediaType.APPLICATION_OCTET_STREAM;
        };
    }
}
