package com.xiaoliao.api.chat.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 后端统一 AI 对话请求（DeepSeek 通道）：
 * 原来小程序/网页前端直连 DeepSeek，现在改由后端调用，前端只带消息与历史。
 */
@Data
public class AiChatRequest {

    /** 本轮用户消息 */
    private String message;

    /** 历史消息 [{role:user/assistant, content}]，与前端 chatHistory 结构一致 */
    private List<Map<String, String>> conversationHistory = new ArrayList<>();

    /** 系统提示词：前端已拼好时原样透传（网页版）；为空则后端按小辽人设+RAG 组装（小程序，与之前一模一样） */
    private String system;

    /** 生成上限，默认 300（对齐小程序 askDeepSeek 的 max_tokens） */
    private Integer maxTokens;

    /** 温度，默认 0.7（与前端一致） */
    private Double temperature;
}
