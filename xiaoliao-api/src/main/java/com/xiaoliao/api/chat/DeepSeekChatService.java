package com.xiaoliao.api.chat;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.xiaoliao.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * 后端 DeepSeek 直连通道：把原来小程序/网页前端直连 DeepSeek 的方式原样搬到后端。
 * <p>参数与前端一致：deepseek-chat / max_tokens(默认300) / temperature 0.7 / stream=false / 30s 超时；
 * system 为空时按小程序 buildSystemPrompt（小辽人设+CBT 准则+RAG 知识库注入）组装，保证回复与之前一模一样。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeepSeekChatService {

    private final RagService ragService;

    @Value("${xiaoliao.deepseek.api-key:}")
    private String apiKey;

    @Value("${xiaoliao.deepseek.model:deepseek-chat}")
    private String model;

    @Value("${xiaoliao.deepseek.http-url:https://api.deepseek.com/chat/completions}")
    private String httpUrl;

    @Value("${xiaoliao.deepseek.timeout:30s}")
    private Duration timeout;

    /** 与前端 askDeepSeek 等价：system + 历史 + 本轮 user → DeepSeek → 文本 */
    public String chat(String message, List<Map<String, String>> history, String system,
                       Integer maxTokens, Double temperature) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new BusinessException(500, "AI 服务未配置：请在后端设置 DEEPSEEK_API_KEY");
        }
        String sys = (system != null && !system.isBlank()) ? system : buildSystemPrompt(message);

        JSONArray msgs = new JSONArray();
        msgs.set(new JSONObject().set("role", "system").set("content", sys));
        if (history != null) {
            int size = history.size();
            int from = Math.max(0, size - 24);
            for (int i = from; i < size; i++) {
                Map<String, String> h = history.get(i);
                if (h == null) {
                    continue;
                }
                String role = h.get("role");
                String content = h.get("content");
                if (role == null || content == null || content.isBlank()) {
                    continue;
                }
                if (!"user".equals(role) && !"assistant".equals(role) && !"system".equals(role)) {
                    continue;
                }
                msgs.set(new JSONObject().set("role", role).set("content", content));
            }
        }
        msgs.set(new JSONObject().set("role", "user").set("content", message));

        JSONObject payload = new JSONObject()
                .set("model", model)
                .set("messages", msgs)
                .set("max_tokens", maxTokens == null ? 300 : maxTokens)
                .set("temperature", temperature == null ? 0.7 : temperature)
                .set("stream", false);

        try (HttpResponse resp = HttpRequest.post(httpUrl)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .timeout((int) timeout.toMillis())
                .body(payload.toString())
                .execute()) {
            String body = resp.body();
            if (resp.getStatus() != 200) {
                log.error("[DeepSeek] 接口返回 {}: {}", resp.getStatus(),
                        body != null && body.length() > 500 ? body.substring(0, 500) : body);
                throw new BusinessException(502, "小辽暂时连不上大脑，请稍后再试");
            }
            JSONObject json = JSONUtil.parseObj(body);
            JSONArray choices = json.getJSONArray("choices");
            if (choices == null || choices.isEmpty()) {
                return "";
            }
            String txt = choices.getJSONObject(0).getJSONObject("message").getStr("content");
            return txt == null ? "" : txt.trim();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("[DeepSeek] 请求失败: {}", e.getMessage(), e);
            throw new BusinessException(502, "小辽暂时连不上大脑，请稍后再试");
        }
    }

    /** 与小程序 common.js buildSystemPrompt 文案完全一致（人设 + CBT 准则 + RAG 注入） */
    public String buildSystemPrompt(String userText) {
        String sys =
                "你是\"小辽\"，时光花园里陪伴老人的 AI 伙伴，30多岁，像一位贴心的晚辈。" +
                "用户是城市独居中老年人（50+）。\n" +
                "【说话风格】口语化短句，温暖肯定；不说教、不劝慰（禁\"你要坚强\"\"想开点\"\"别难过\"）；" +
                "不用\"叔叔阿姨大爷奶奶\"这类称谓，用\"您\"或画像里的名字更亲切；表情克制（🌼偶尔）。\n" +
                "【禁止】不要用括号写动作/状态描写（如\"（轻轻放下茶杯）\"），不要描写神态、环境，直接说话。\n" +
                "【陪伴方式】倾听优先，先接情绪再谈事情；围绕两大叙事引导讲述：人生时光（我的一辈子）和年代记忆（我最怀念的年代）；" +
                "讲完给一句\"换个说法\"式肯定。\n" +
                "【边界】不冒充专业人士；老人表达持续悲伤/绝望/轻生倾向时，停止分析，温和引导联系家人或就医；" +
                "不编造、不评判老人的人生选择。\n" +
                "【禁止编造往事·重要】绝不虚构老人的过去。只有老人在本次对话里明确说过的内容，才能回引；" +
                "不要编造\"您上次说…\"\"您之前提过…\"\"您是不是…过\"这类没发生过的细节。想不起来就不提，直接陪老人聊当下。\n" +
                "【CBT 对话精要·不露痕迹】①先接情绪再谈事情 ②命名情绪 ③区分事实和想法 ④给想法找证据 " +
                "⑤换个说法（\"那会儿您已经尽力了\"）⑥低落时提议\"出去走走看看太阳？\"⑦收得住（不想谈就\"好，不说了，我陪着您\"）。" +
                "绝不说\"这是认知行为疗法\"\"根据心理学\"。";
        List<String[]> docs = ragService.search(userText);
        if (!docs.isEmpty()) {
            StringBuilder ctx = new StringBuilder();
            for (int i = 0; i < docs.size(); i++) {
                String[] d = docs.get(i);
                if (i > 0) {
                    ctx.append("\n\n");
                }
                ctx.append("[资料").append(i + 1).append("：").append(d[0]).append("]\n").append(d[1]);
            }
            sys += "\n\n以下是相关知识库资料（CBT 方法/情绪知识/对话指南），请自然地运用其中的方法回应老人，不要生硬引用、不要说\"根据资料\"：\n\n" + ctx;
        }
        return sys;
    }
}
