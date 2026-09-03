package com.xiaoliao.api.audio;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.xiaoliao.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Base64;

/**
 * 阿里云百炼 qwen3-asr-flash 语音识别（HTTP）。
 * <p>
 * 微信录音 mp3 直接 base64 上传识别，无需转码/重采样，返回识别文字。
 * 模型与聊天大模型共用同一把阿里云百炼 API Key，语音按音频时长单独计费（有免费额度）。
 */
@Slf4j
@Service
public class DashScopeAsrService {

    @Value("${xiaoliao.asr.api-key:}")
    private String apiKey;

    @Value("${xiaoliao.asr.model:qwen3-asr-flash}")
    private String model;

    @Value("${xiaoliao.asr.http-url:https://dashscope.aliyuncs.com/api/v1/services/aigc/multimodal-generation/generation}")
    private String httpUrl;

    @Value("${xiaoliao.asr.timeout:90s}")
    private Duration timeout;

    /**
     * mp3 录音 → 识别文字（可能为空串，表示没听清）
     */
    public String transcribe(byte[] audioBytes) {
        return transcribe(audioBytes, "audio/mp3");
    }

    /**
     * 录音 → 识别文字；dataMime 形如 audio/mp3、audio/wav、audio/webm
     */
    public String transcribe(byte[] audioBytes, String dataMime) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new BusinessException(500, "语音识别未配置：请设置阿里云百炼 QWEN_API_KEY");
        }
        if (audioBytes == null || audioBytes.length == 0) {
            throw new BusinessException(400, "录音内容为空，请重新录制");
        }

        String b64 = Base64.getEncoder().encodeToString(audioBytes);
        JSONObject payload = new JSONObject()
                .set("model", model)
                .set("input", new JSONObject().set("messages", new JSONArray().set(new JSONObject()
                        .set("role", "user")
                        .set("content", new JSONArray().set(new JSONObject()
                                .set("audio", "data:" + dataMime + ";base64," + b64))))))
                .set("parameters", new JSONObject().set("result_format", "text"));

        try (HttpResponse resp = HttpRequest.post(httpUrl)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .timeout((int) timeout.toMillis())
                .body(payload.toString())
                .execute()) {
            String body = resp.body();
            if (resp.getStatus() != 200) {
                log.error("语音识别接口返回 {}: {}", resp.getStatus(), body);
                throw new BusinessException(500, extractMessage(body));
            }
            JSONObject json = JSONUtil.parseObj(body);
            JSONArray choices = json.getJSONObject("output").getJSONArray("choices");
            if (choices == null || choices.isEmpty()) {
                return "";
            }
            JSONArray content = choices.getJSONObject(0).getJSONObject("message").getJSONArray("content");
            if (content == null || content.isEmpty()) {
                return ""; // 无有效语音（没说话/纯音乐）
            }
            return content.getJSONObject(0).getStr("text", "").trim();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("语音识别请求失败: {}", e.getMessage(), e);
            throw new BusinessException(500, "语音识别服务连接失败，请稍后再试");
        }
    }

    private String extractMessage(String body) {
        try {
            JSONObject o = JSONUtil.parseObj(body);
            String msg = o.getStr("message");
            return msg == null || msg.isBlank() ? "语音识别失败" : msg;
        } catch (Exception e) {
            return "语音识别失败";
        }
    }
}
