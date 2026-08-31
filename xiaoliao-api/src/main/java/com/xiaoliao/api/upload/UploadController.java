package com.xiaoliao.api.upload;

import com.xiaoliao.api.config.AuthContext;
import com.xiaoliao.common.dto.Result;
import com.xiaoliao.common.exception.BusinessException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 图片上传 — 存应用服务器本地磁盘（不依赖对象存储），Nginx/静态映射对外提供访问
 */
@Slf4j
@Tag(name = "上传", description = "图片上传（本地磁盘存储）")
@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class UploadController {

    private static final Set<String> ALLOWED_EXT = Set.of("jpg", "jpeg", "png", "webp", "gif");
    private static final long MAX_SIZE = 10L * 1024 * 1024; // 10MB

    @Value("${xiaoliao.upload.dir:./uploads}")
    private String uploadDir;

    @Value("${xiaoliao.upload.base-url:http://localhost:8080}")
    private String baseUrl;

    @Operation(summary = "上传图片", description = "小程序拍照/相册上传，返回可访问的图片 URL")
    @PostMapping
    public Result<Map<String, Object>> upload(
            @Parameter(hidden = true) @RequestAttribute(AuthContext.USER_ID_ATTR) String userId,
            @RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "请选择要上传的图片");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new BusinessException(400, "图片太大了，请控制在 10MB 以内");
        }
        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String ext = original.contains(".")
                ? original.substring(original.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT)
                : "";
        if (!ALLOWED_EXT.contains(ext)) {
            throw new BusinessException(400, "只支持 jpg / png / webp / gif 图片");
        }
        try {
            String sub = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
            java.nio.file.Path dir = java.nio.file.Paths.get(uploadDir, sub).toAbsolutePath().normalize();
            java.nio.file.Files.createDirectories(dir);
            String name = UUID.randomUUID().toString().replace("-", "") + "." + ext;
            file.transferTo(dir.resolve(name).toFile());

            String url = baseUrl.replaceAll("/+$", "") + "/uploads/" + sub + "/" + name;
            log.info("图片上传成功: userId={}, url={}", userId, url);
            Map<String, Object> data = new HashMap<>();
            data.put("url", url);
            return Result.ok(data);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("图片上传失败: userId={}, 根因={}", userId, e.getMessage(), e);
            throw new BusinessException(500, "图片保存失败，请稍后再试");
        }
    }
}

