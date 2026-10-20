package com.mqy.olearning.service;

import com.mqy.olearning.common.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

/**
 * 本地磁盘文件存储
 * 一期直接用 upload/ 目录，报告里注明可平滑迁移 MinIO / OSS
 */
@Slf4j
@Service
public class FileService {

    @Value("${app.upload.path}")
    private String uploadPath;

    @Value("${app.upload.url-prefix}")
    private String urlPrefix;

    public Map<String, Object> upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }
        String original = StringUtils.cleanPath(
                file.getOriginalFilename() == null ? "file" : file.getOriginalFilename());
        String ext = "";
        int dot = original.lastIndexOf('.');
        if (dot > -1) {
            ext = original.substring(dot).toLowerCase();
        }

        // 按年月分目录，避免单目录文件过多
        String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
        File dir = new File(uploadPath, dateDir);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new BusinessException("创建上传目录失败");
        }

        String fileName = UUID.randomUUID().toString().replace("-", "") + ext;
        File dest = new File(dir, fileName);
        try {
            file.transferTo(dest.getAbsoluteFile());
        } catch (Exception e) {
            log.error("文件保存失败", e);
            throw new BusinessException("文件保存失败");
        }

        return Map.of(
                "url", urlPrefix + dateDir + "/" + fileName,
                "name", original,
                "size", file.getSize(),
                "type", ext.replace(".", "")
        );
    }
}
