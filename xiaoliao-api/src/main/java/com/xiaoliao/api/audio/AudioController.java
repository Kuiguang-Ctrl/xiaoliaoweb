package com.xiaoliao.api.audio;

import com.xiaoliao.api.config.AuthContext;
import com.xiaoliao.common.dto.Result;
import com.xiaoliao.common.exception.BusinessException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 语音识别 — 小程序按住说话录音(mp3)上传，后端转文字后走聊天链路
 */
@Slf4j
@Tag(name = "语音识别", description = "录音上传 → 阿里云百炼 paraformer 语音转文字")
@RestController
@RequestMapping("/api/audio")
@RequiredArgsConstructor
public class AudioController {

    private static final long MAX_SIZE = 10L * 1024 * 1024; // 10MB（60s 录音足够）

    /** 按文件名后缀推断音频 data mime；网页录音传 wav，小程序传 mp3 */
    private static String mimeOf(String filename) {
        if (filename == null) return "audio/mp3";
        int i = filename.lastIndexOf('.');
        if (i < 0) return "audio/mp3";
        String ext = filename.substring(i + 1).toLowerCase(Locale.ROOT);
        switch (ext) {
            case "wav": return "audio/wav";
            case "webm": return "audio/webm";
            case "mp4":
            case "m4a": return "audio/mp4";
            case "ogg":
            case "opus": return "audio/ogg";
            case "amr": return "audio/amr";
            case "flac": return "audio/flac";
            case "aac": return "audio/aac";
            default: return "audio/mp3";
        }
    }

    private final DashScopeAsrService asrService;

    @Operation(summary = "语音转文字", description = "接收小程序录音 mp3，返回识别文字")
    @PostMapping("/asr")
    public Result<Map<String, Object>> asr(
            @Parameter(hidden = true) @RequestAttribute(AuthContext.USER_ID_ATTR) String userId,
            @RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "请先按住说话录音");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new BusinessException(400, "录音太大了，请控制在 10MB 以内");
        }
        try {
            byte[] audio = file.getBytes();
            String text = asrService.transcribe(audio, mimeOf(file.getOriginalFilename()));
            Map<String, Object> data = new HashMap<>();
            data.put("text", text);
            log.info("语音识别成功: userId={}, textLen={}", userId, text.length());
            return Result.ok(data);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("语音识别失败: userId={}, 根因={}", userId, e.getMessage(), e);
            throw new BusinessException(500, "语音识别失败，请稍后再试");
        }
    }
}
