package com.xiaoliao.api.wecom;

import com.xiaoliao.api.wecom.WeComAsyncHandler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.cp.api.WxCpService;
import me.chanjar.weixin.cp.bean.message.WxCpXmlMessage;
import me.chanjar.weixin.cp.util.crypto.WxCpCryptUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 企业微信回调接口 — 接收用户消息、返回 AI 回复
 */
@Tag(name = "企业微信回调")
@Slf4j
@Tag(name = "企业微信回调", description = "企微消息回调、URL 验证")
@RestController
@RequiredArgsConstructor
public class WeComController {

    private final WxCpService wxCpService;
    private final WeComAsyncHandler asyncHandler;

    @Value("${wecom.kf.open-kfid}")
    private String openKfid;

    @Value("${wecom.miniprogram.appid:}")
    private String appId;

    /**
     * URL 验证：企微配置回调时发 GET 请求验证
     */
    @Operation(summary = "企微回调 URL 验证", description = "企微配置回调地址时发的 GET 验证请求")
@GetMapping(value = {"/wecom/callback", "/wxcallback"}, produces = "text/plain")
    public String verify(@RequestParam("msg_signature") String signature,
                         @RequestParam("timestamp") String timestamp,
                         @RequestParam("nonce") String nonce,
                         @RequestParam("echostr") String echostr) {
        WxCpCryptUtil cryptUtil = new WxCpCryptUtil(wxCpService.getWxCpConfigStorage());
        return cryptUtil.decrypt(echostr);
    }

    /**
     * 接收消息：解析后立刻返回 success，异步处理 AI 回复
     * 企微要求 5 秒内响应，否则会重试
     */
    @Operation(summary = "接收企微用户消息", description = "企微用户发消息回调，解析后异步调 AI 回复，立即返回 success")
@PostMapping(value = {"/wecom/callback", "/wxcallback"}, produces = "text/plain")
    public String callback(@RequestBody String xmlBody,
                           @RequestParam("msg_signature") String signature,
                           @RequestParam("timestamp") String timestamp,
                           @RequestParam("nonce") String nonce) {
        try {
            WxCpXmlMessage msg = WxCpXmlMessage.fromEncryptedXml(
                    xmlBody, wxCpService.getWxCpConfigStorage(),
                    signature, timestamp, nonce);

            String openid = msg.getExternalUserId();
            String msgType = msg.getMsgType();
            String event = msg.getEvent();

            log.info("收到企微回调: openid={}, msgType={}, event={}",
                    openid, msgType, event);

            if ("text".equals(msgType)) {
                // 自建应用消息回调（旧）：正文直接随回调下发
                asyncHandler.handle(msg, openKfid, appId);
            } else if ("event".equals(msgType) && "kf_msg_or_event".equals(event)) {
                // 微信客服(kf)回调：只通知有新消息，正文需用回调 token 调 sync_msg 拉取
                Map<String, Object> allFields = msg.getAllFieldsMap();
                String kfToken = strField(allFields, "Token");
                String kfOpenKfid = strField(allFields, "OpenKfid");
                if (kfOpenKfid.isBlank()) {
                    kfOpenKfid = openKfid; // 兜底用配置的 open-kfid
                }
                asyncHandler.handleKfEvent(kfOpenKfid, kfToken, appId);
            }

        } catch (Exception e) {
            log.error("解析企微消息异常", e);
        }

        // 立刻返回 success，不等待 AI 回复
        return "success";
    }

    /**
     * 从回调原始字段 map 里取值（兼容 Key 大小写），找不到返回空串
     */
    private String strField(Map<String, Object> fields, String key) {
        if (fields == null) {
            return "";
        }
        for (Map.Entry<String, Object> e : fields.entrySet()) {
            if (e.getKey() != null && e.getKey().equalsIgnoreCase(key) && e.getValue() != null) {
                return String.valueOf(e.getValue());
            }
        }
        return "";
    }
}
