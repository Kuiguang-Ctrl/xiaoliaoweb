package com.xiaoliao.api.chat;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xiaoliao.api.chat.entity.ProactiveMessage;
import com.xiaoliao.api.chat.repository.ProactiveMessageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * AI 主动消息：小辽主动问候，聊天页打开时拉取（每天每人最多一条）
 * <p>问候由 AI 引擎生成（绕开对话管线），失败/坏内容时回退本地模板，稳定不阻塞。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProactiveMessageService {

    /** 小辽主动问候模板（按日期轮换） */
    private static final String[] TEMPLATE_GREETINGS = {
            "早上好呀，我是小辽～ 今天想先和您聊聊天，或者去签个到记录一下心情，都可以的。",
            "您好呀，小辽在这儿陪着您。新的一天，想聊什么都可以～ 也别忘了今天的心情签到哦。",
            "嗨，我是小辽～ 有什么想说的都可以告诉我，也可以先去看看今天的脑力小游戏。",
            "早安，小辽来啦～ 今天过得怎么样？方便的话先签个到，告诉小辽您今天的心情吧。",
    };

    /** 疑似"引擎没听清/坏回复"的内容特征（测试数据误入库时自动丢弃重建） */
    private static final String[] BAD_CONTENT_PATTERNS = {"没听清", "请再说一次", "接稳", "刚才没能"};

    /** 判断一段主动消息内容是否为疑似坏回复（空内容也算坏） */
    public static boolean isBadContent(String content) {
        if (content == null || content.isBlank()) {
            return true;
        }
        for (String pattern : BAD_CONTENT_PATTERNS) {
            if (content.contains(pattern)) {
                return true;
            }
        }
        return false;
    }

    private final ProactiveMessageMapper mapper;
    private final AiService aiService;

    /**
     * 取今天的主动问候；当天还没有则生成一条入库（每天每人最多一条）
     */
    public ProactiveMessage getOrCreateTodayGreeting(String userId) {
        ProactiveMessage existing = findTodayGreeting(userId);
        if (existing != null) {
            if (!isBadContent(existing.getContent())) {
                return existing;
            }
            log.warn("[主动问候] 当天记录疑似坏内容，删除重建 userId={} id={} content={}",
                    userId, existing.getId(), existing.getContent());
            mapper.deleteById(existing.getId());
        }

        // AI 生成优先（根据实际情况说口语化问候）；不可用/坏内容时模板兜底
        String content = aiService.greeting(userId);
        if (content == null || isBadContent(content)) {
            log.warn("[主动问候] AI 生成不可用或内容异常，使用模板 userId={}", userId);
            content = generateGreeting();
        }
        return insertGreeting(userId, content);
    }

    /** 查当天第一条主动问候；没有返回 null */
    public ProactiveMessage findTodayGreeting(String userId) {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        return mapper.selectOne(new LambdaQueryWrapper<ProactiveMessage>()
                .eq(ProactiveMessage::getUserId, userId)
                .ge(ProactiveMessage::getCreatedAt, todayStart)
                .orderByAsc(ProactiveMessage::getId)
                .last("LIMIT 1"));
    }

    /** 生成一条问候并入库（scene=greeting） */
    public ProactiveMessage insertGreeting(String userId, String content) {
        ProactiveMessage msg = new ProactiveMessage();
        msg.setUserId(userId);
        msg.setScene("greeting");
        msg.setContent(content);
        msg.setCreatedAt(LocalDateTime.now());
        mapper.insert(msg);
        log.info("[主动问候] 生成问候 userId={}", userId);
        return msg;
    }

    /** 本地模板按日期轮换（引擎实现 /v1/greeting 后替换为 AI 生成，失败回退模板） */
    public String generateGreeting() {
        return TEMPLATE_GREETINGS[
                (int) (LocalDate.now().toEpochDay() % TEMPLATE_GREETINGS.length)];
    }
}
