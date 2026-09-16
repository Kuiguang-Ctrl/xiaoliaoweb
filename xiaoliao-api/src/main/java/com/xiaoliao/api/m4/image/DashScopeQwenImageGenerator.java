package com.xiaoliao.api.m4.image;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.xiaoliao.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 阿里云百炼 Qwen-Image 文生图（HTTP 直连）。
 * 与 ASR（qwen3-asr-flash）共用同一把百炼 Key：xiaoliao.image.api-key 缺省时回退 xiaoliao.asr.api-key。
 * 生成的图立即下载落盘到 uploads/images/yyyyMM/ 并返回本项目绝对 URL（第三方链接有有效期，不能直接入库）。
 */
@Slf4j
@Component
public class DashScopeQwenImageGenerator implements ImageGenerator {

    /** 风格 → 追加的画面尾缀（默认老照片写实；老人明确说彩色/素描再换） */
    private static final Map<String, String> STYLE_SUFFIX = new LinkedHashMap<>();

    static {
        STYLE_SUFFIX.put("old_photo", "。老照片写实风格：泛黄做旧、略带颗粒与岁月感，人物自然真实，环境细节符合年代");
        STYLE_SUFFIX.put("color", "。彩色写实照片风格，光线自然，细节清晰");
        STYLE_SUFFIX.put("sketch", "。黑白素描风格，线条柔和");
    }

    @Value("${xiaoliao.image.api-key:}")
    private String imageApiKey;

    @Value("${xiaoliao.asr.api-key:}")
    private String asrApiKey;

    @Value("${xiaoliao.image.model:qwen-image}")
    private String model;

    @Value("${xiaoliao.image.http-url:https://dashscope.aliyuncs.com/api/v1/services/aigc/multimodal-generation/generation}")
    private String httpUrl;

    @Value("${xiaoliao.image.timeout:60s}")
    private Duration timeout;

    @Value("${xiaoliao.image.size:1280*720}")
    private String size;

    @Value("${xiaoliao.upload.dir:./uploads}")
    private String uploadDir;

    @Value("${xiaoliao.upload.base-url:http://localhost:8080}")
    private String uploadBaseUrl;

    @Override
    public String generate(String prompt, String style) {
        String apiKey = (imageApiKey == null || imageApiKey.isBlank()) ? asrApiKey : imageApiKey;
        if (apiKey == null || apiKey.isBlank()) {
            throw new BusinessException(500, "画图服务未配置：请设置阿里云百炼 QWEN_API_KEY");
        }
        String full = prompt == null ? "" : prompt.trim();
        if (full.isEmpty()) {
            throw new BusinessException(400, "画面描述不能为空");
        }
        String suffix = STYLE_SUFFIX.getOrDefault(style == null ? "" : style, STYLE_SUFFIX.get("old_photo"));
        full = full + suffix;

        JSONObject payload = new JSONObject()
                .set("model", model)
                .set("input", new JSONObject().set("messages", new JSONArray().set(new JSONObject()
                        .set("role", "user")
                        .set("content", new JSONArray().set(new JSONObject().set("text", full))))))
                .set("parameters", new JSONObject().set("size", size).set("n", 1));

        String remoteUrl;
        try (HttpResponse resp = HttpRequest.post(httpUrl)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .timeout((int) timeout.toMillis())
                .body(payload.toString())
                .execute()) {
            String body = resp.body();
            if (resp.getStatus() != 200) {
                log.error("文生图接口返回 {}: {}", resp.getStatus(), body);
                throw new BusinessException(500, extractMessage(body));
            }
            remoteUrl = extractImage(body);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("文生图请求失败: {}", e.getMessage(), e);
            throw new BusinessException(500, "画图服务连接失败，请稍后再试");
        }
        if (remoteUrl == null || remoteUrl.isBlank()) {
            throw new BusinessException(500, "画图服务没有返回图片，请稍后再试");
        }
        return persist(remoteUrl);
    }

    /** 兼容解析 output.choices[0].message.content[0].image / image_url */
    private String extractImage(String body) {
        try {
            JSONObject json = JSONUtil.parseObj(body);
            JSONArray choices = json.getJSONObject("output").getJSONArray("choices");
            if (choices == null || choices.isEmpty()) {
                return null;
            }
            JSONArray content = choices.getJSONObject(0).getJSONObject("message").getJSONArray("content");
            if (content == null) {
                return null;
            }
            for (Object o : content) {
                if (!(o instanceof JSONObject item)) {
                    continue;
                }
                String img = item.getStr("image");
                if (img == null || img.isBlank()) {
                    img = item.getStr("image_url");
                }
                if (img != null && !img.isBlank()) {
                    return img.trim();
                }
            }
            log.warn("文生图返回结构未识别: {}", body.length() > 500 ? body.substring(0, 500) : body);
            return null;
        } catch (Exception e) {
            log.warn("解析文生图响应失败: {}", e.getMessage());
            return null;
        }
    }

    /** 第三方图有有效期：立刻下载并按图片头落盘为 jpg/png，返回本项目 URL */
    private String persist(String remote) {
        try {
            byte[] bytes;
            if (remote.startsWith("data:image/")) {
                int comma = remote.indexOf(',');
                if (comma < 0) {
                    throw new BusinessException(500, "画图返回数据格式异常");
                }
                bytes = Base64.getDecoder().decode(remote.substring(comma + 1));
            } else {
                bytes = HttpRequest.get(remote).timeout((int) timeout.toMillis()).execute().bodyBytes();
            }
            if (bytes == null || bytes.length < 8) {
                throw new BusinessException(500, "图片下载结果为空");
            }
            boolean jpg = (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8;
            String ext = jpg ? "jpg" : "png";
            String sub = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
            Path dir = Paths.get(uploadDir, "images", sub).toAbsolutePath().normalize();
            Files.createDirectories(dir);
            String name = UUID.randomUUID().toString().replace("-", "") + "." + ext;
            Files.write(dir.resolve(name), bytes);
            String url = uploadBaseUrl.replaceAll("/+$", "") + "/uploads/images/" + sub + "/" + name;
            log.info("文生图落盘成功: url={}", url);
            return url;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("文生图落盘失败: {}", e.getMessage(), e);
            throw new BusinessException(500, "图片保存失败，请稍后再试");
        }
    }

    private String extractMessage(String body) {
        try {
            JSONObject o = JSONUtil.parseObj(body);
            String msg = o.getStr("message");
            String code = o.getStr("code");
            if (msg == null || msg.isBlank()) {
                return code == null || code.isBlank() ? "画图服务调用失败" : "画图服务调用失败(" + code + ")";
            }
            return msg;
        } catch (Exception e) {
            return "画图服务调用失败";
        }
    }
}
