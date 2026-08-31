package com.xiaoliao.api.chat;

import com.xiaoliao.api.chat.dto.ChatRequest;
import com.xiaoliao.api.chat.dto.ChatResponse;
import com.xiaoliao.api.config.AuthContext;
import com.xiaoliao.api.m4.dto.MatchImageRequest;
import com.xiaoliao.api.m4.dto.MomentGenerateRequest;
import com.xiaoliao.api.m4.dto.PhotoUploadRequest;
import com.xiaoliao.api.m4.service.M4Service;
import com.xiaoliao.api.m4.vo.MatchResultVO;
import com.xiaoliao.api.m4.vo.MomentVO;
import com.xiaoliao.api.m4.vo.PhotoVO;
import com.xiaoliao.api.m4.vo.StockImageVO;
import com.xiaoliao.api.metrics.ApiMetric;
import com.xiaoliao.common.dto.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * AI 对话接口 — 前端小程序调 AI 客服，Java 转发给 Python 智能体引擎
 * <p>
 * M4 本地兜底路由（引擎未接入前）：收到照片/命中 M4 关键词时，直接走 M4Service 返回卡片；
 * 引擎接入后改为按引擎 intent 分发，此路由仅保留降级作用。
 */
@Tag(name = "AI 对话", description = "前端调 AI 客服，Java 转发给 Python 引擎")
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final AiService aiService;
    private final M4Service m4Service;

    private static final Pattern P_MOMENT = Pattern.compile(".*(朋友圈|文案|再改改|重写|换一条).*");
    private static final Pattern P_MATCH = Pattern.compile(".*(配.*图|当年的图|以前的图|以前是|以前的样子|示意图|再找找|再换).*");
    private static final Pattern P_STORY = Pattern.compile(".*(讲故事|讲讲|说说.*以前|回忆).*");
    private static final Pattern P_NO_IMAGE = Pattern.compile(".*(不用图|不要图).*");

    @Operation(summary = "AI 对话", description = "发送用户消息（可带图片 URL），返回 AI 回复文本、意图与可选卡片。userId 由鉴权自动带入，无需传")
    @PostMapping
    @ApiMetric("chat.ai")
    public Result<Map<String, Object>> chat(
            @Parameter(hidden = true) @RequestAttribute(AuthContext.USER_ID_ATTR) String userId,
            @RequestBody ChatRequest request) {
        boolean hasImage = request.getImages() != null && !request.getImages().isEmpty();

        // ① 收到照片 → 保存照片 + 三个动作按钮（引擎接入后由 intent=m4_photo_received 驱动）
        if (hasImage && request.getImages().get(0) != null) {
            PhotoUploadRequest photoReq = new PhotoUploadRequest();
            photoReq.setUrl(request.getImages().get(0));
            photoReq.setSource("user");
            photoReq.setIsIllustration(0);
            PhotoVO photo = m4Service.savePhoto(userId, photoReq);

            Map<String, Object> card = new HashMap<>();
            card.put("type", "buttons");
            card.put("title", "收到照片啦！您想做什么？");
            List<Map<String, String>> buttons = new ArrayList<>();
            buttons.add(Map.of("key", "match_photo", "label", "配一张当年的图"));
            buttons.add(Map.of("key", "moment", "label", "写朋友圈文案"));
            buttons.add(Map.of("key", "story", "label", "讲讲这张照片的故事"));
            card.put("buttons", buttons);
            return Result.ok(chatData("收到照片啦！您想做什么？", "m4_photo_received", card));
        }

        String message = request.getMessage() == null ? "" : request.getMessage().trim();

        // ② M4 关键词兜底路由（引擎接入后替换为 intent 分发）
        Map<String, Object> local = localM4Route(userId, message);
        if (local != null) {
            return Result.ok(local);
        }

        // ③ 正常对话 → Python 引擎
        ChatResponse resp = aiService.chat(userId, message, request.getConversationHistory(),
                request.getSessionId(), request.getImages());
        Map<String, Object> data = chatData(resp.getReply(), resp.getIntent(), resp.getCard());
        data.put("inspection", resp.getInspection());
        data.put("recommendation", resp.getRecommendation());
        return Result.ok(data);
    }

    /** M4 本地兜底：命中返回卡片数据，未命中返回 null */
    private Map<String, Object> localM4Route(String userId, String message) {
        if (P_NO_IMAGE.matcher(message).matches()) {
            return chatData("好，不配图也行，光发您这张照片就很好！", "m4_match_photo", null);
        }
        if (P_MOMENT.matcher(message).matches()) {
            List<MomentVO> moments = m4Service.generateMoments(userId, new MomentGenerateRequest());
            Map<String, Object> card = new HashMap<>();
            card.put("type", "moments");
            card.put("title", "帮您写了几条，选一条或让我再改改：");
            List<Map<String, Object>> items = new ArrayList<>();
            for (MomentVO m : moments) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", m.getId());
                item.put("style", m.getStyle());
                item.put("text", m.getContent());
                item.put("selected", m.getSelected());
                items.add(item);
            }
            card.put("moments", items);
            return chatData("帮您写了几条，选一条或让我再改改：", "m4_moment", card);
        }
        if (P_MATCH.matcher(message).matches()) {
            MatchImageRequest req = new MatchImageRequest();
            req.setDescription(message);
            MatchResultVO match = m4Service.matchImage(userId, req);
            if (match.isMatched()) {
                Map<String, Object> card = new HashMap<>();
                card.put("type", "images");
                card.put("title", "找了几张当年样子的示意图，您看像不像？");
                List<Map<String, Object>> images = new ArrayList<>();
                for (StockImageVO img : match.getSuggestions()) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("url", img.getUrl());
                    item.put("title", img.getScene());
                    item.put("license", img.getLicense());
                    images.add(item);
                }
                card.put("images", images);
                return chatData("找了几张当年样子的示意图，您看像不像？", "m4_match_photo", card);
            }
            // 搜不到：明确告诉老人，给"换个说法/不用图"按钮，绝不硬配图
            Map<String, Object> card = new HashMap<>();
            card.put("type", "buttons");
            card.put("title", "暂时没找到合适的示意图，您换个说法试试？比如“土房”“绿皮火车”这样的词～ 或者不用图，光发照片也行。");
            List<Map<String, String>> buttons = new ArrayList<>();
            buttons.add(Map.of("key", "match_retry", "label", "换个说法再找找"));
            buttons.add(Map.of("key", "match_skip", "label", "不用图"));
            card.put("buttons", buttons);
            return chatData("暂时没找到合适的示意图，您换个说法试试？比如“土房”“绿皮火车”这样的词～ 或者不用图，光发照片也行。", "m4_match_photo", card);
        }
        if (P_STORY.matcher(message).matches()) {
            return chatData("好呀，您慢慢讲，一次说一段就行。讲完我帮您记下来、润色润色。", "m4_story", null);
        }
        return null;
    }

    private Map<String, Object> chatData(String reply, String intent, Map<String, Object> card) {
        Map<String, Object> data = new HashMap<>();
        data.put("reply", reply);
        data.put("intent", intent);
        data.put("card", card);
        return data;
    }
}


