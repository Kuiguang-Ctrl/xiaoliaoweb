package com.xiaoliao.api.m3.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xiaoliao.api.checkin.entity.CheckinRecord;
import com.xiaoliao.api.checkin.repository.CheckinMapper;
import com.xiaoliao.api.m2.entity.M2UserGameAchievement;
import com.xiaoliao.api.m2.entity.M2UserGameRecord;
import com.xiaoliao.api.m2.enums.GameStatusEnum;
import com.xiaoliao.api.m2.mapper.M2UserGameAchievementMapper;
import com.xiaoliao.api.m2.mapper.M2UserGameRecordMapper;
import com.xiaoliao.api.m3.dto.CollectRequest;
import com.xiaoliao.api.m3.dto.GoodThingSubmitRequest;
import com.xiaoliao.api.m3.dto.GratitudeSaveRequest;
import com.xiaoliao.api.m3.entity.M3AchievementCollect;
import com.xiaoliao.api.m3.entity.M3GoodThingRecord;
import com.xiaoliao.api.m3.entity.M3GratitudeNote;
import com.xiaoliao.api.m3.mapper.M3AchievementCollectMapper;
import com.xiaoliao.api.m3.mapper.M3GoodThingRecordMapper;
import com.xiaoliao.api.m3.mapper.M3GratitudeNoteMapper;
import com.xiaoliao.api.m3.service.M3Service;
import com.xiaoliao.api.m3.vo.AchievementVO;
import com.xiaoliao.api.m3.vo.GoodThingMonthVO;
import com.xiaoliao.api.m3.vo.GoodThingRecordVO;
import com.xiaoliao.api.m3.vo.GoodThingSubmitVO;
import com.xiaoliao.api.m3.vo.GoodThingTodayVO;
import com.xiaoliao.api.m3.vo.GratitudeVO;
import com.xiaoliao.api.m3.vo.WeeklySummaryVO;
import com.xiaoliao.api.user.UserService;
import com.xiaoliao.api.util.RedisLock;
import com.xiaoliao.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * M3 积极心理练习实现
 * <p>
 * 设计说明：
 * - 三件好事：每天最多 3 条，当天可删，多鼓励少惩罚
 * - 感恩留言：纯文本保存 + 生成可复制文案，发送给家人留到 M8
 * - 成就墙：游戏成就复用 m2_user_game_achievement，里程碑实时计算不落库，
 *   收藏状态单独存 m3_achievement_collect
 * - 每周小结：后端模板规则生成，零 AI 依赖
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class M3ServiceImpl implements M3Service {

    private final M3GoodThingRecordMapper goodThingMapper;
    private final M3GratitudeNoteMapper gratitudeMapper;
    private final M3AchievementCollectMapper collectMapper;
    private final M2UserGameRecordMapper recordMapper;
    private final M2UserGameAchievementMapper gameAchievementMapper;
    private final CheckinMapper checkinMapper;
    private final UserService userService;
    private final RedisLock redisLock;

    private static final int DAILY_LIMIT = 3;
    private static final String GOOD_THING_LOCK_PREFIX = "m3:good:lock:";
    private static final Duration GOOD_THING_LOCK_TIMEOUT = Duration.ofSeconds(30);
    private static final String SOURCE_GAME = "game";
    private static final String SOURCE_MILESTONE = "milestone";
    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // ==================== 三件好事 ====================

    @Override
    public GoodThingSubmitVO submitGoodThing(String userId, GoodThingSubmitRequest request) {
        requireUser(userId);
        LocalDate today = LocalDate.now();

        // 分布式锁串行化「查询 + 插入」，防止并发提交突破每天 3 条上限
        String lockKey = GOOD_THING_LOCK_PREFIX + userId + ":" + today.format(DAY_FMT);
        String requestId = null;
        boolean redisUnavailable = false;
        try {
            requestId = redisLock.tryLock(lockKey, GOOD_THING_LOCK_TIMEOUT);
        } catch (Exception e) {
            redisUnavailable = true;
            log.warn("Redis 不可用，跳过分布式锁直接执行: {}", e.getMessage());
        }
        if (requestId == null && !redisUnavailable) {
            throw new BusinessException(409, "提交太频繁啦，稍等一下再试试～");
        }

        try {
            long todayCount = goodThingMapper.selectCount(
                    new LambdaQueryWrapper<M3GoodThingRecord>()
                            .eq(M3GoodThingRecord::getUserId, userId)
                            .eq(M3GoodThingRecord::getRecordDate, today));
            if (todayCount >= DAILY_LIMIT) {
                throw new BusinessException(409, "今天已经记满三件好事啦，明天再来记录吧～");
            }

            M3GoodThingRecord record = new M3GoodThingRecord();
            record.setUserId(userId);
            record.setContent(request.getContent().trim());
            record.setRecordDate(today);
            record.setCreateTime(LocalDateTime.now());
            record.setUpdateTime(LocalDateTime.now());
            goodThingMapper.insert(record);
            log.info("好事记录成功: userId={}, id={}", userId, record.getId());

            GoodThingSubmitVO vo = new GoodThingSubmitVO();
            vo.setId(record.getId());
            vo.setContent(record.getContent());
            vo.setRecordDate(today.format(DAY_FMT));
            vo.setTodayCount((int) todayCount + 1);
            vo.setEncourageMsg(encourageForToday(vo.getTodayCount()));
            return vo;
        } finally {
            if (requestId != null) {
                redisLock.unlock(lockKey, requestId);
            }
        }
    }

    @Override
    public GoodThingTodayVO todayGoodThings(String userId) {
        requireUser(userId);
        List<M3GoodThingRecord> records = goodThingMapper.selectList(
                new LambdaQueryWrapper<M3GoodThingRecord>()
                        .eq(M3GoodThingRecord::getUserId, userId)
                        .eq(M3GoodThingRecord::getRecordDate, LocalDate.now())
                        .orderByAsc(M3GoodThingRecord::getCreateTime));

        GoodThingTodayVO vo = new GoodThingTodayVO();
        vo.setRecords(records.stream().map(this::toRecordVO).collect(Collectors.toList()));
        vo.setTodayCount(records.size());
        vo.setDailyLimit(DAILY_LIMIT);
        return vo;
    }

    @Override
    public GoodThingMonthVO monthGoodThings(String userId, String month) {
        requireUser(userId);
        YearMonth ym;
        if (month == null || month.isBlank()) {
            ym = YearMonth.now();
        } else {
            try {
                ym = YearMonth.parse(month);
            } catch (DateTimeParseException e) {
                throw new BusinessException(400, "month 格式应为 yyyy-MM");
            }
        }

        LocalDate first = ym.atDay(1);
        LocalDate last = ym.atEndOfMonth();
        List<M3GoodThingRecord> records = goodThingMapper.selectList(
                new LambdaQueryWrapper<M3GoodThingRecord>()
                        .eq(M3GoodThingRecord::getUserId, userId)
                        .ge(M3GoodThingRecord::getRecordDate, first)
                        .le(M3GoodThingRecord::getRecordDate, last)
                        .orderByDesc(M3GoodThingRecord::getRecordDate)
                        .orderByDesc(M3GoodThingRecord::getCreateTime));

        GoodThingMonthVO vo = new GoodThingMonthVO();
        vo.setMonth(ym.toString());
        vo.setTotalCount(records.size());
        vo.setRecords(records.stream().map(this::toRecordVO).collect(Collectors.toList()));
        return vo;
    }

    @Override
    public void deleteGoodThing(String userId, Long id) {
        requireUser(userId);
        M3GoodThingRecord record = goodThingMapper.selectById(id);
        if (record == null || !record.getUserId().equals(userId)) {
            throw new BusinessException(404, "记录不存在");
        }
        if (!record.getRecordDate().equals(LocalDate.now())) {
            throw new BusinessException(400, "只有今天记录的好事可以删除哦");
        }
        goodThingMapper.deleteById(id);
        log.info("好事删除成功: userId={}, id={}", userId, id);
    }

    // ==================== 感恩留言 ====================

    @Override
    public GratitudeVO saveGratitude(String userId, GratitudeSaveRequest request) {
        requireUser(userId);
        M3GratitudeNote note = new M3GratitudeNote();
        note.setUserId(userId);
        note.setTargetName(request.getTargetName().trim());
        note.setContent(request.getContent().trim());
        note.setShared(0);
        note.setCreateTime(LocalDateTime.now());
        note.setUpdateTime(LocalDateTime.now());
        gratitudeMapper.insert(note);
        log.info("感恩留言保存成功: userId={}, id={}", userId, note.getId());

        GratitudeVO vo = new GratitudeVO();
        vo.setId(note.getId());
        vo.setTargetName(note.getTargetName());
        vo.setContent(note.getContent());
        vo.setCreateTime(note.getCreateTime().format(TIME_FMT));
        vo.setShared(note.getShared());
        vo.setShareText("我想对" + note.getTargetName() + "说：" + note.getContent());
        return vo;
    }

    @Override
    public List<GratitudeVO> listGratitude(String userId) {
        requireUser(userId);
        List<M3GratitudeNote> notes = gratitudeMapper.selectList(
                new LambdaQueryWrapper<M3GratitudeNote>()
                        .eq(M3GratitudeNote::getUserId, userId)
                        .orderByDesc(M3GratitudeNote::getCreateTime)
                        .last("LIMIT 50"));
        return notes.stream().map(n -> {
            GratitudeVO vo = new GratitudeVO();
            vo.setId(n.getId());
            vo.setTargetName(n.getTargetName());
            vo.setContent(n.getContent());
            vo.setCreateTime(n.getCreateTime() != null ? n.getCreateTime().format(TIME_FMT) : null);
            vo.setShared(n.getShared());
            vo.setShareText("我想对" + n.getTargetName() + "说：" + n.getContent());
            return vo;
        }).collect(Collectors.toList());
    }

    // ==================== 每周开心小结 ====================

    @Override
    public WeeklySummaryVO weeklySummary(String userId) {
        requireUser(userId);
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(DayOfWeek.MONDAY);
        LocalDate weekEnd = weekStart.plusDays(6);

        WeeklySummaryVO vo = new WeeklySummaryVO();
        vo.setWeekStart(weekStart.format(DAY_FMT));
        vo.setWeekEnd(weekEnd.format(DAY_FMT));

        vo.setGoodThingCount(goodThingMapper.selectCount(
                new LambdaQueryWrapper<M3GoodThingRecord>()
                        .eq(M3GoodThingRecord::getUserId, userId)
                        .ge(M3GoodThingRecord::getRecordDate, weekStart)
                        .le(M3GoodThingRecord::getRecordDate, weekEnd)));

        vo.setGratitudeCount(gratitudeMapper.selectCount(
                new LambdaQueryWrapper<M3GratitudeNote>()
                        .eq(M3GratitudeNote::getUserId, userId)
                        .ge(M3GratitudeNote::getCreateTime, weekStart.atStartOfDay())
                        .le(M3GratitudeNote::getCreateTime, weekEnd.atTime(LocalTime.MAX))));

        vo.setGameRoundCount(recordMapper.selectCount(
                new LambdaQueryWrapper<M2UserGameRecord>()
                        .eq(M2UserGameRecord::getUserId, userId)
                        .ge(M2UserGameRecord::getPlayDate, weekStart)
                        .le(M2UserGameRecord::getPlayDate, weekEnd)
                        .eq(M2UserGameRecord::getGameStatus, GameStatusEnum.FINISHED.getValue())));

        vo.setAchievementCount(gameAchievementMapper.selectCount(
                new LambdaQueryWrapper<M2UserGameAchievement>()
                        .eq(M2UserGameAchievement::getUserId, userId)
                        .ge(M2UserGameAchievement::getCreateTime, weekStart.atStartOfDay())
                        .le(M2UserGameAchievement::getCreateTime, weekEnd.atTime(LocalTime.MAX))));

        vo.setCheckinDays(checkinMapper.selectCount(
                new LambdaQueryWrapper<CheckinRecord>()
                        .eq(CheckinRecord::getUserId, userId)
                        .ge(CheckinRecord::getCheckinDate, weekStart)
                        .le(CheckinRecord::getCheckinDate, weekEnd)));

        vo.setSummary(buildWeeklySummary(vo));
        vo.setEncourageMsg(encourageForWeek(vo));
        vo.setTips(buildTips(vo));
        return vo;
    }

    // ==================== 成就墙 ====================

    @Override
    public List<AchievementVO> achievements(String userId) {
        requireUser(userId);
        List<AchievementVO> list = new ArrayList<>();

        // 1. 游戏成就（复用 M2 表）
        List<M2UserGameAchievement> gameAchs = gameAchievementMapper.selectList(
                new LambdaQueryWrapper<M2UserGameAchievement>()
                        .eq(M2UserGameAchievement::getUserId, userId)
                        .orderByDesc(M2UserGameAchievement::getCreateTime)
                        .last("LIMIT 50"));
        for (M2UserGameAchievement a : gameAchs) {
            AchievementVO vo = new AchievementVO();
            vo.setSourceType(SOURCE_GAME);
            vo.setSourceId(String.valueOf(a.getId()));
            vo.setTitle("游戏成就");
            vo.setDescription(a.getAchievementDesc());
            vo.setEmoji(starEmoji(a.getStar()));
            vo.setStar(a.getStar());
            vo.setCreateTime(a.getCreateTime() != null ? a.getCreateTime().format(TIME_FMT) : null);
            list.add(vo);
        }

        // 2. 动态里程碑（实时计算，不落库）
        long totalGood = goodThingMapper.selectCount(
                new LambdaQueryWrapper<M3GoodThingRecord>()
                        .eq(M3GoodThingRecord::getUserId, userId));
        if (totalGood >= 10) {
            list.add(milestone("goodthing-10", "幸福收集家", "累计记录了 10 件好事", "🌟"));
        }

        long totalGrat = gratitudeMapper.selectCount(
                new LambdaQueryWrapper<M3GratitudeNote>()
                        .eq(M3GratitudeNote::getUserId, userId));
        if (totalGrat >= 3) {
            list.add(milestone("gratitude-3", "暖心传递者", "送出了 3 份感恩留言", "💌"));
        }

        if (computeCheckinStreak(userId) >= 7) {
            list.add(milestone("checkin-streak-7", "签到达人", "连续签到 7 天", "📅"));
        }

        // 3. 收藏状态回填
        Set<String> collectedKeys = collectMapper.selectList(
                        new LambdaQueryWrapper<M3AchievementCollect>()
                                .eq(M3AchievementCollect::getUserId, userId))
                .stream()
                .map(c -> c.getSourceType() + ":" + c.getSourceId())
                .collect(Collectors.toSet());
        for (AchievementVO vo : list) {
            vo.setCollected(collectedKeys.contains(vo.getSourceType() + ":" + vo.getSourceId()));
        }
        return list;
    }

    @Override
    public Map<String, Object> collect(String userId, CollectRequest request) {
        requireUser(userId);
        boolean doCollect = request.getCollect() == null || request.getCollect();

        LambdaQueryWrapper<M3AchievementCollect> wrapper =
                new LambdaQueryWrapper<M3AchievementCollect>()
                        .eq(M3AchievementCollect::getUserId, userId)
                        .eq(M3AchievementCollect::getSourceType, request.getSourceType())
                        .eq(M3AchievementCollect::getSourceId, request.getSourceId());

        if (doCollect) {
            Long exists = collectMapper.selectCount(wrapper);
            if (exists == null || exists == 0) {
                M3AchievementCollect c = new M3AchievementCollect();
                c.setUserId(userId);
                c.setSourceType(request.getSourceType());
                c.setSourceId(request.getSourceId());
                c.setCreateTime(LocalDateTime.now());
                collectMapper.insert(c);
            }
        } else {
            collectMapper.delete(wrapper);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("sourceType", request.getSourceType());
        data.put("sourceId", request.getSourceId());
        data.put("collected", doCollect);
        return data;
    }

    // ==================== 私有辅助 ====================

    private void requireUser(String userId) {
        if (userService.getById(userId) == null) {
            throw new BusinessException(401, "登录已失效，请重新进入");
        }
    }

    private GoodThingRecordVO toRecordVO(M3GoodThingRecord r) {
        GoodThingRecordVO vo = new GoodThingRecordVO();
        vo.setId(r.getId());
        vo.setContent(r.getContent());
        vo.setRecordDate(r.getRecordDate() != null ? r.getRecordDate().format(DAY_FMT) : null);
        vo.setCreateTime(r.getCreateTime() != null ? r.getCreateTime().format(TIME_FMT) : null);
        return vo;
    }

    private AchievementVO milestone(String sourceId, String title, String description, String emoji) {
        AchievementVO vo = new AchievementVO();
        vo.setSourceType(SOURCE_MILESTONE);
        vo.setSourceId(sourceId);
        vo.setTitle(title);
        vo.setDescription(description);
        vo.setEmoji(emoji);
        vo.setStar(null);
        return vo;
    }

    private String starEmoji(Integer star) {
        if (star == null || star <= 0) {
            return "🏅";
        }
        return "⭐".repeat(Math.min(star, 3));
    }

    private String encourageForToday(int todayCount) {
        return switch (todayCount) {
            case 1 -> "真好！记下一件好事，心情也亮堂堂～";
            case 2 -> "第二件啦！今天的好事可真不少～";
            case 3 -> "三件好事集齐！今天是被幸福包围的一天～";
            default -> "今天也要开开心心呀～";
        };
    }

    private String buildWeeklySummary(WeeklySummaryVO vo) {
        long activity = vo.getGoodThingCount() + vo.getGratitudeCount()
                + vo.getGameRoundCount() + vo.getAchievementCount();
        if (activity == 0 && vo.getCheckinDays() == 0) {
            return "这一周还没有留下记录。没关系，从今天记一件小事开始吧～";
        }
        List<String> parts = new ArrayList<>();
        if (vo.getGoodThingCount() > 0) {
            parts.add("记录了 " + vo.getGoodThingCount() + " 件好事");
        }
        if (vo.getGratitudeCount() > 0) {
            parts.add("送出了 " + vo.getGratitudeCount() + " 份感恩");
        }
        if (vo.getGameRoundCount() > 0) {
            parts.add("动了 " + vo.getGameRoundCount() + " 次脑筋");
        }
        if (vo.getCheckinDays() > 0) {
            parts.add("签到了 " + vo.getCheckinDays() + " 天");
        }
        if (vo.getAchievementCount() > 0) {
            parts.add("收获了 " + vo.getAchievementCount() + " 个成就");
        }
        return "这一周，你" + String.join("、", parts) + "。日子过得有滋有味！";
    }

    private String encourageForWeek(WeeklySummaryVO vo) {
        if (vo.getGoodThingCount() >= 3) {
            return "好事满满的一周，真了不起！";
        }
        if (vo.getGoodThingCount() >= 1) {
            return "已经有了好的开始，下周继续～";
        }
        return "慢慢来，下周一起加油～";
    }

    private List<String> buildTips(WeeklySummaryVO vo) {
        List<String> tips = new ArrayList<>();
        if (vo.getGoodThingCount() < 3) {
            tips.add("下周试试每天记一件小事，凑齐三件好事～");
        }
        if (vo.getGameRoundCount() < 3) {
            tips.add("再来两局脑力游戏，大脑越用越灵光～");
        }
        if (vo.getCheckinDays() < 5) {
            tips.add("每天来签到，看看自己的心情晴雨表～");
        }
        if (tips.isEmpty()) {
            tips.add("这周表现真好，下周继续保持～");
        }
        return tips;
    }

    /** 连续签到天数（从今天往回数，与 M1 口径一致） */
    private int computeCheckinStreak(String userId) {
        LocalDate today = LocalDate.now();
        List<CheckinRecord> records = checkinMapper.selectList(
                new LambdaQueryWrapper<CheckinRecord>()
                        .eq(CheckinRecord::getUserId, userId)
                        .ge(CheckinRecord::getCheckinDate, today.minusDays(60))
                        .orderByDesc(CheckinRecord::getCheckinDate));
        Set<LocalDate> dates = records.stream()
                .map(CheckinRecord::getCheckinDate)
                .collect(Collectors.toSet());
        int streak = 0;
        LocalDate cursor = today;
        while (dates.contains(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }
}