package com.xiaoliao.api.chat.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 主动消息推送请求（测试/运营用）：推给当前用户，在线走 WebSocket 实时到达，离线仅入库
 */
@Data
public class PushRequest {

    @NotBlank(message = "内容不能为空")
    private String content;

    /** 场景：greeting/checkin_reminder/mood_care/inactive_recall/festival/game_recommend */
    private String scene;
}
