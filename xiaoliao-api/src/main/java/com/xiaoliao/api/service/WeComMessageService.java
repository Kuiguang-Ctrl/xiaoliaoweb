package com.xiaoliao.api.service;

import com.xiaoliao.common.enums.IntentType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.cp.api.WxCpService;
import me.chanjar.weixin.cp.bean.kf.WxCpKfMsgSendRequest;
import me.chanjar.weixin.cp.bean.kf.msg.WxCpKfMiniProgramMsg;
import me.chanjar.weixin.cp.bean.kf.msg.WxCpKfTextMsg;
import org.springframework.stereotype.Service;

/**
 * 企业微信客服消息服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WeComMessageService {

    private final WxCpService wxCpService;

    /**
     * 发送文本消息
     */
    public void sendText(String openid, String openKfid, String content) {
        WxCpKfTextMsg text = new WxCpKfTextMsg();
        text.setContent(content);

        WxCpKfMsgSendRequest msg = new WxCpKfMsgSendRequest();
        msg.setToUser(openid);
        msg.setOpenKfid(openKfid);
        msg.setMsgType("text");
        msg.setText(text);

        try {
            wxCpService.getKfService().sendMsg(msg);
        } catch (Exception e) {
            log.error("发送文本消息失败: openid={}", openid, e);
        }
    }

    /**
     * 发送小��序卡片
     */
    public void sendMiniProgramCard(String openid, String openKfid,
                                     String title, String pagePath,
                                     String appId, String thumbMediaId) {
        WxCpKfMiniProgramMsg mini = new WxCpKfMiniProgramMsg();
        mini.setAppId(appId);
        mini.setTitle(title);
        mini.setPagePath(pagePath);
        mini.setThumbMediaId(thumbMediaId);

        WxCpKfMsgSendRequest msg = new WxCpKfMsgSendRequest();
        msg.setToUser(openid);
        msg.setOpenKfid(openKfid);
        msg.setMsgType("miniprogram");
        msg.setMiniProgram(mini);

        try {
            wxCpService.getKfService().sendMsg(msg);
        } catch (Exception e) {
            log.error("发送小程序卡片失败: openid={}", openid, e);
        }
    }

    /**
     * 根据意图类型发送对应的回复 + 卡片
     */
    public void sendIntentReply(String openid, String openKfid,
                                 String textReply, IntentType intent,
                                 String appId, String thumbMediaId) {
        // 1. 先发文本
        sendText(openid, openKfid, textReply);

        // 2. 如果需要卡片，发卡片
        if (intent.needsCard()) {
            sendMiniProgramCard(
                    openid, openKfid,
                    intent.getLabel(),
                    intent.getPagePath(),
                    appId,
                    thumbMediaId
            );
        }
    }
}
