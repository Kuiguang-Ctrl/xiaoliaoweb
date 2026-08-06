package com.xiaoliao.api.controller;

import com.xiaoliao.api.service.WeComAsyncHandler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.cp.api.WxCpService;
import me.chanjar.weixin.cp.bean.message.WxCpXmlMessage;
import me.chanjar.weixin.cp.util.crypto.WxCpCryptUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

/**
 * 企业微信回调接口 — 接收用户消息、返回 AI 回复
 */
@Tag(name = "企业微信回调")
@Slf4j
@Tag(name = "企业微信回调", description = "企微消息回调、URL 验证")
@RestController
@RequestMapping("/wecom")
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
    @GetMapping(value = "/callback", produces = "text/plain")
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
    @PostMapping(value = "/callback", produces = "text/plain")
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

            log.info("收到用户消息: openid={}, type={}, content={}",
                    openid, msgType, msg.getContent());

            // 只处理文本消息
            if ("text".equals(msgType)) {
                asyncHandler.handle(msg, openKfid, appId);
            }

        } catch (Exception e) {
            log.error("解析企微消息异常", e);
        }

        // 立刻返回 success，不等待 AI 回复
        return "success";
    }
}
