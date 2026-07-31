package com.xiaoliao.api.controller;

import com.xiaoliao.api.service.WeComAsyncHandler;
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
@Slf4j
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
