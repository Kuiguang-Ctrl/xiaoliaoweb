package com.xiaoliao.api.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xiaoliao.api.dto.checkin.CalendarVO;
import com.xiaoliao.api.dto.checkin.CheckinRequest;
import com.xiaoliao.api.dto.checkin.CheckinResultVO;
import com.xiaoliao.api.dto.checkin.TodayVO;
import com.xiaoliao.api.entity.checkin.CheckinRecord;
import com.xiaoliao.api.repository.checkin.CheckinMapper;
import com.xiaoliao.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 签到服务 — 快捷签到 / 今日状态 / 情绪月历
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CheckinService {

    private final CheckinMapper checkinMapper;
    private final UserService userService;

    /** 合法情绪档位 */
    private static final Set<String> MOODS = Set.of("sunny", "cloudy", "overcast", "rain", "storm");
    /** 负面情绪 */
    private static final Set<String> NEGATIVE_MOODS = Set.of("rain", "storm");

    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 快捷签到
     */
    public CheckinResultVO checkIn(String userId, CheckinRequest request) {
        if (userService.getById(userId) == null) {
            throw new BusinessException(401, "登录已失效，请重新进入");
        }

        String mood = request.getMood();
        if (mood != null && !MOODS.contains(mood)) {
            throw new BusinessException(400, "mood 取值非法");
        }

        LocalDate today = LocalDate.now();

        // 一天只能签一次
        CheckinRecord existed = findByDate(userId, today);
        if (existed != null) {
            throw new BusinessException(409, "今天已经签到啦，明天再来");
        }

        CheckinRecord record = new CheckinRecord();
        record.setUserId(userId);
        record.setMood(mood);
        record.setMoodNote(request.getMoodNote());
        record.setCheckinDate(today);
        record.setCreatedAt(LocalDateTime.now());
        checkinMapper.insert(record);
        log.info("用户签到成功: userId={}, mood={}", userId, mood);

        StreakInfo streaks = computeStreaks(userId, today);
        boolean careAlert = mood != null
                && NEGATIVE_MOODS.contains(mood)
                && streaks.negativeStreak >= 3;

        return CheckinResultVO.builder()
                .checkinDate(today.format(DAY_FMT))
                .mood(mood)
                .consecutiveDays(streaks.streak)
                .feedback(feedbackFor(mood))
                .careAlert(careAlert)
                .build();
    }

    /**
     * 今日状态（小程序首页）
     */
    public TodayVO today(String userId) {
        LocalDate today = LocalDate.now();
        CheckinRecord record = findByDate(userId, today);
        // 已签到：连续数算到今天；未签到：连续数算到昨天为止（今天的还没断）
        boolean checkedIn = record != null;
        int streak = computeStreaks(userId, checkedIn ? today : today.minusDays(1)).streak;

        return TodayVO.builder()
                .date(today.format(DAY_FMT))
                .weekday(today.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.CHINA))
                .season(seasonCn(today))
                .greeting(greetingByHour(LocalTime.now().getHour()))
                .weather(null)
                .checkedIn(checkedIn)
                .todayMood(record != null ? record.getMood() : null)
                .consecutiveDays(streak)
                .build();
    }

    /**
     * 情绪月历
     */
    public CalendarVO calendar(String userId, String month) {
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

        List<CheckinRecord> records = checkinMapper.selectList(
                new LambdaQueryWrapper<CheckinRecord>()
                        .eq(CheckinRecord::getUserId, userId)
                        .ge(CheckinRecord::getCheckinDate, first)
                        .le(CheckinRecord::getCheckinDate, last)
                        .orderByAsc(CheckinRecord::getCheckinDate));

        List<CalendarVO.DayVO> days = new ArrayList<>();
        int positive = 0;
        int negative = 0;
        int noMood = 0;

        for (CheckinRecord r : records) {
            String mood = r.getMood();
            days.add(CalendarVO.DayVO.builder()
                    .date(r.getCheckinDate().format(DAY_FMT))
                    .mood(mood)
                    .moodNote(r.getMoodNote())
                    .build());
            if (mood == null) {
                noMood++;
            } else if (NEGATIVE_MOODS.contains(mood)) {
                negative++;
            } else {
                positive++;
            }
        }

        CalendarVO.CalendarStatsVO stats = CalendarVO.CalendarStatsVO.builder()
                .totalDays(days.size())
                .positiveDays(positive)
                .negativeDays(negative)
                .noMoodDays(noMood)
                .build();

        return CalendarVO.builder()
                .month(ym.toString())
                .checkins(days)
                .stats(stats)
                .build();
    }

    // ───────── 私有方法 ─────────

    private CheckinRecord findByDate(String userId, LocalDate date) {
        return checkinMapper.selectOne(
                new LambdaQueryWrapper<CheckinRecord>()
                        .eq(CheckinRecord::getUserId, userId)
                        .eq(CheckinRecord::getCheckinDate, date)
                        .last("LIMIT 1"));
    }

    /**
     * 计算连续签到天数 + 连续负面情绪天数（截至 end 日期）
     */
    private StreakInfo computeStreaks(String userId, LocalDate end) {
        List<CheckinRecord> records = checkinMapper.selectList(
                new LambdaQueryWrapper<CheckinRecord>()
                        .eq(CheckinRecord::getUserId, userId)
                        .ge(CheckinRecord::getCheckinDate, end.minusDays(60))
                        .orderByDesc(CheckinRecord::getCheckinDate));

        Map<LocalDate, CheckinRecord> byDate = new HashMap<>();
        for (CheckinRecord r : records) {
            byDate.put(r.getCheckinDate(), r);
        }

        int streak = 0;
        LocalDate cursor = end;
        while (byDate.containsKey(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }

        int negativeStreak = 0;
        cursor = end;
        while (true) {
            CheckinRecord r = byDate.get(cursor);
            if (r == null || !NEGATIVE_MOODS.contains(r.getMood())) {
                break;
            }
            negativeStreak++;
            cursor = cursor.minusDays(1);
        }

        return new StreakInfo(streak, negativeStreak);
    }

    private String feedbackFor(String mood) {
        if (mood == null) {
            return "今天也要元气满满呀！";
        }
        return switch (mood) {
            case "sunny", "cloudy" -> "今天也要开开心心呀～";
            case "overcast" -> "阴天也要有个好心情～";
            case "rain", "storm" -> "抱抱你，小辽一直陪着你～";
            default -> "今天也要元气满满呀！";
        };
    }

    private String seasonCn(LocalDate date) {
        int m = date.getMonthValue();
        if (m >= 3 && m <= 5) {
            return "春";
        }
        if (m >= 6 && m <= 8) {
            return "夏";
        }
        if (m >= 9 && m <= 11) {
            return "秋";
        }
        return "冬";
    }

    private String greetingByHour(int hour) {
        if (hour >= 5 && hour < 11) {
            return "早上好呀，今天也要开开心心";
        }
        if (hour >= 11 && hour < 13) {
            return "中午好，记得好好吃饭～";
        }
        if (hour >= 13 && hour < 18) {
            return "下午好，累了吧，歇一歇～";
        }
        if (hour >= 18 && hour < 23) {
            return "晚上好，今天辛苦啦～";
        }
        return "夜深啦，早点休息哦～";
    }

    /** 连续天数统计结果 */
    private static final class StreakInfo {
        final int streak;
        final int negativeStreak;

        StreakInfo(int streak, int negativeStreak) {
            this.streak = streak;
            this.negativeStreak = negativeStreak;
        }
    }
}
