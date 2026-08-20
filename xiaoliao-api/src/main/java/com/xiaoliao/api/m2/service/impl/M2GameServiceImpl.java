package com.xiaoliao.api.m2.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xiaoliao.api.m2.constant.M2Constants;
import com.xiaoliao.api.m2.entity.M2GameInfo;
import com.xiaoliao.api.m2.entity.M2GameLevel;
import com.xiaoliao.api.m2.entity.M2UserDailyTrain;
import com.xiaoliao.api.m2.entity.M2UserGameAchievement;
import com.xiaoliao.api.m2.entity.M2UserGameRecord;
import com.xiaoliao.api.m2.enums.CognitiveDomainEnum;
import com.xiaoliao.api.m2.enums.GameStatusEnum;
import com.xiaoliao.api.m2.mapper.M2GameInfoMapper;
import com.xiaoliao.api.m2.mapper.M2GameLevelMapper;
import com.xiaoliao.api.m2.mapper.M2UserDailyTrainMapper;
import com.xiaoliao.api.m2.mapper.M2UserGameAchievementMapper;
import com.xiaoliao.api.m2.mapper.M2UserGameRecordMapper;
import com.xiaoliao.api.m2.service.M2GameService;
import com.xiaoliao.api.m2.vo.DailyTrainVO;
import com.xiaoliao.api.m2.vo.GameFinishResultVO;
import com.xiaoliao.api.m2.vo.GameFinishVO;
import com.xiaoliao.api.m2.vo.GameListVO;
import com.xiaoliao.api.m2.vo.GameStartVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class M2GameServiceImpl implements M2GameService {

    private final M2GameInfoMapper gameInfoMapper;
    private final M2GameLevelMapper gameLevelMapper;
    private final M2UserGameRecordMapper recordMapper;
    private final M2UserDailyTrainMapper dailyTrainMapper;
    private final M2UserGameAchievementMapper achievementMapper;

    // ==================== 列表 ====================

    @Override
    public List<GameListVO> listGames(String userId) {
        List<M2GameInfo> games = gameInfoMapper.selectList(
                new LambdaQueryWrapper<M2GameInfo>()
                        .eq(M2GameInfo::getIsEnable, M2Constants.ENABLE)
                        .orderByAsc(M2GameInfo::getSort)
        );
        if (CollUtil.isEmpty(games)) {
            return List.of();
        }

        List<GameListVO> result = new ArrayList<>(games.size());
        for (M2GameInfo g : games) {
            GameListVO vo = new GameListVO();
            vo.setId(g.getId());
            vo.setGameCode(g.getGameCode());
            vo.setGameName(g.getGameName());
            vo.setCognitiveDomain(g.getCognitiveDomain());
            vo.setGameDesc(g.getGameDesc());
            vo.setSingleMinute(g.getSingleMinute());

            int levelNo = resolveRecommendLevelNo(userId, g.getId());
            M2GameLevel level = findLevel(g.getId(), levelNo);
            vo.setRecommendLevelNo(levelNo);
            vo.setRecommendLevelName(level != null ? level.getLevelName() : "入门");
            result.add(vo);
        }
        return result;
    }

    // ==================== 开始一局 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GameStartVO start(String userId, Long gameId) {
        M2GameInfo game = requireEnabledGame(gameId);

        M2UserGameRecord playing = recordMapper.selectOne(
                new LambdaQueryWrapper<M2UserGameRecord>()
                        .eq(M2UserGameRecord::getUserId, userId)
                        .eq(M2UserGameRecord::getGameId, gameId)
                        .eq(M2UserGameRecord::getGameStatus, GameStatusEnum.PLAYING.getValue())
                        .last("LIMIT 1")
        );
        if (playing != null) {
            playing.setGameStatus(GameStatusEnum.ABANDONED.getValue());
            playing.setEndTime(LocalDateTime.now());
            recordMapper.updateById(playing);
            log.info("用户 {} 有未完成的对局 {}，已自动放弃", userId, playing.getId());
        }

        int levelNo = resolveRecommendLevelNo(userId, gameId);
        M2GameLevel level = requireLevel(gameId, levelNo);

        LocalDateTime now = LocalDateTime.now();
        M2UserGameRecord record = new M2UserGameRecord();
        record.setUserId(userId);
        record.setGameId(gameId);
        record.setLevelId(level.getId());
        record.setPreLevelNo(levelNo);
        record.setNextLevelNo(levelNo);
        record.setCognitiveType(game.getCognitiveDomain());
        record.setPlayDate(now.toLocalDate());
        record.setStartTime(now);
        record.setGameStatus(GameStatusEnum.PLAYING.getValue());
        recordMapper.insert(record);

        GameStartVO vo = new GameStartVO();
        vo.setRecordId(record.getId());
        vo.setGameId(gameId);
        vo.setGameName(game.getGameName());
        vo.setCognitiveDomain(game.getCognitiveDomain());
        vo.setLevelId(level.getId());
        vo.setLevelNo(levelNo);
        vo.setLevelName(level.getLevelName());
        vo.setConfigJson(level.getConfigJson());
        vo.setSingleMinute(game.getSingleMinute());
        vo.setMaxMinute(game.getMaxMinute());
        vo.setErrorless(Objects.equals(game.getErrorPunish(), 0));
        vo.setStartTime(now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        return vo;
    }

    // ==================== 结束一局（自适应核心） ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GameFinishResultVO finish(String userId, GameFinishVO vo) {
        M2UserGameRecord record = recordMapper.selectById(vo.getRecordId());
        if (record == null || !Objects.equals(record.getUserId(), userId)) {
            throw new IllegalArgumentException("对局记录不存在");
        }
        if (!Objects.equals(record.getGameStatus(), GameStatusEnum.PLAYING.getValue())) {
            throw new IllegalArgumentException("该局已结束，请勿重复提交");
        }
        // 校验答题数量合法范围。
        // 注意：翻牌/找不同/分类等玩法中"答错"是额外尝试次数，可超过总题数（如 4 对配对翻了 5 次错的），
        // 因此这里只校验正确数不超过总题数，不校验 correct+wrong<=total。
        if (vo.getTotalQuestion() <= 0) {
            throw new IllegalArgumentException("总题数必须大于 0");
        }
        if (vo.getCorrectQuestion() < 0 || vo.getWrongQuestion() < 0) {
            throw new IllegalArgumentException("答题数量不能为负");
        }
        if (vo.getCorrectQuestion() > vo.getTotalQuestion()) {
            throw new IllegalArgumentException("答对数不能超过总题数");
        }

        M2GameInfo game = requireEnabledGame(record.getGameId());
        M2GameLevel currentLevel = requireLevelById(record.getLevelId());

        int passRate = calcPassRate(vo.getCorrectQuestion(), vo.getTotalQuestion());
        int star = calcStar(passRate, game.getStarTotal());
        boolean finished = Objects.equals(vo.getGameStatus(), GameStatusEnum.FINISHED.getValue());

        int nextLevelNo = record.getPreLevelNo();
        boolean levelUp = false;
        boolean levelDown = false;
        if (finished) {
            int[] adaptive = adaptiveNext(
                    record.getPreLevelNo(),
                    passRate,
                    currentLevel.getPassRateUp(),
                    currentLevel.getPassRateDown(),
                    record.getGameId()
            );
            nextLevelNo = adaptive[0];
            levelUp = adaptive[1] == 1;
            levelDown = adaptive[1] == -1;
        }

        LocalDateTime now = LocalDateTime.now();
        record.setEndTime(now);
        record.setCostSecond(vo.getCostSecond());
        record.setTotalQuestion(vo.getTotalQuestion());
        record.setCorrectQuestion(vo.getCorrectQuestion());
        record.setWrongQuestion(vo.getWrongQuestion());
        record.setPassRate(passRate);
        record.setStar(star);
        record.setNextLevelNo(nextLevelNo);
        record.setGameStatus(vo.getGameStatus());
        recordMapper.updateById(record);

        upsertDailyTrain(userId, record, finished);

        Long achievementId = null;
        if (finished && star > 0) {
            achievementId = createAchievement(userId, record, game, star);
        }

        M2GameLevel nextLevel = findLevel(record.getGameId(), nextLevelNo);

        GameFinishResultVO result = new GameFinishResultVO();
        result.setRecordId(record.getId());
        result.setPassRate(passRate);
        result.setStar(star);
        result.setPreLevelNo(record.getPreLevelNo());
        result.setNextLevelNo(nextLevelNo);
        result.setNextLevelName(nextLevel != null ? nextLevel.getLevelName() : null);
        result.setLevelUp(levelUp);
        result.setLevelDown(levelDown);
        result.setCognitiveTip(CognitiveDomainEnum.tipOf(record.getCognitiveType()));
        result.setEncourageMsg(buildEncourageMsg(finished, passRate, levelUp, levelDown));
        result.setAchievementId(achievementId);
        return result;
    }

    // ==================== 日汇总查询 ====================

    @Override
    public DailyTrainVO dailyTrain(String userId, String dateStr) {
        LocalDate date;
        if (StrUtil.isBlank(dateStr)) {
            date = LocalDate.now();
        } else {
            try {
                date = LocalDate.parse(dateStr);
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("日期格式错误，请使用 yyyy-MM-dd 格式");
            }
        }

        M2UserDailyTrain daily = dailyTrainMapper.selectByUserAndDate(userId, date);

        DailyTrainVO vo = new DailyTrainVO();
        vo.setTrainDate(date.toString());
        if (daily == null) {
            vo.setTotalSecond(0);
            vo.setGameCount(0);
            vo.setFinishRound(0);
            vo.setAbandonRound(0);
            vo.setMemoryScore(0);
            vo.setAttentionScore(0);
            vo.setReasonScore(0);
            vo.setLanguageScore(0);
            vo.setBalanceLevel(0);
            vo.setBalanceTip("今天还没有训练，动动脑会更开心哦");
            return vo;
        }
        vo.setTotalSecond(daily.getTotalSecond());
        vo.setGameCount(daily.getGameCount());
        vo.setFinishRound(daily.getFinishRound());
        vo.setAbandonRound(daily.getAbandonRound());
        vo.setMemoryScore(nullToZero(daily.getMemoryScore()));
        vo.setAttentionScore(nullToZero(daily.getAttentionScore()));
        vo.setReasonScore(nullToZero(daily.getReasonScore()));
        vo.setLanguageScore(nullToZero(daily.getLanguageScore()));
        vo.setBalanceLevel(nullToZero(daily.getBalanceLevel()));
        vo.setBalanceTip(buildBalanceTip(vo));
        return vo;
    }

    // ==================== 自适应算法 ====================

    private int[] adaptiveNext(int currentLevelNo, int passRate,
                               Integer passRateUp, Integer passRateDown, Long gameId) {
        int up = passRateUp != null ? passRateUp : M2Constants.DEFAULT_PASS_RATE_UP;
        int down = passRateDown != null ? passRateDown : M2Constants.DEFAULT_PASS_RATE_DOWN;
        int maxLevel = maxEnabledLevelNo(gameId);

        if (passRate >= up) {
            int next = Math.min(currentLevelNo + 1, maxLevel);
            return new int[]{next, next > currentLevelNo ? 1 : 0};
        }
        if (passRate <= down) {
            int next = Math.max(currentLevelNo - 1, M2Constants.MIN_LEVEL_NO);
            return new int[]{next, next < currentLevelNo ? -1 : 0};
        }
        return new int[]{currentLevelNo, 0};
    }

    private int calcPassRate(int correct, int total) {
        if (total <= 0) {
            return 0;
        }
        return (int) Math.round(correct * 100.0 / total);
    }

    private int calcStar(int passRate, Integer starTotal) {
        int max = starTotal != null && starTotal > 0 ? starTotal : M2Constants.MAX_STAR;
        if (passRate >= 90) {
            return Math.min(3, max);
        }
        if (passRate >= 70) {
            return Math.min(2, max);
        }
        if (passRate >= 50) {
            return 1;
        }
        return 0;
    }

    private String buildEncourageMsg(boolean finished, int passRate, boolean levelUp, boolean levelDown) {
        if (!finished) {
            return "没关系，休息一下再来，慢慢来就好";
        }
        if (levelUp) {
            return "太棒了！难度提升一档，大脑又进步了";
        }
        if (levelDown) {
            return "没关系，再练练就熟悉了，加油";
        }
        if (passRate >= 80) {
            return "表现很稳，继续保持这份好状态";
        }
        if (passRate >= 50) {
            return "不错哦，再来一局会更熟练";
        }
        return "完成就是胜利，明天会更好";
    }

    // ==================== 日汇总 & 均衡度 ====================

    private void upsertDailyTrain(String userId, M2UserGameRecord record, boolean finished) {
        LocalDate date = record.getPlayDate();
        M2UserDailyTrain daily = dailyTrainMapper.selectByUserAndDate(userId, date);

        if (daily == null) {
            daily = new M2UserDailyTrain();
            daily.setUserId(userId);
            daily.setTrainDate(date);
            daily.setTotalSecond(0);
            daily.setGameCount(0);
            daily.setFinishRound(0);
            daily.setAbandonRound(0);
            daily.setMemoryScore(0);
            daily.setAttentionScore(0);
            daily.setReasonScore(0);
            daily.setLanguageScore(0);
            daily.setBalanceLevel(0);
        }

        int cost = record.getCostSecond() != null ? record.getCostSecond() : 0;
        daily.setTotalSecond(nullToZero(daily.getTotalSecond()) + cost);

        if (finished) {
            daily.setFinishRound(nullToZero(daily.getFinishRound()) + 1);
            applyDomainScore(daily, record.getCognitiveType(), record.getPassRate());
        } else {
            daily.setAbandonRound(nullToZero(daily.getAbandonRound()) + 1);
        }

        List<M2UserGameRecord> dayRecords = recordMapper.selectList(
                new LambdaQueryWrapper<M2UserGameRecord>()
                        .eq(M2UserGameRecord::getUserId, userId)
                        .eq(M2UserGameRecord::getPlayDate, date)
                        .in(M2UserGameRecord::getGameStatus,
                                GameStatusEnum.FINISHED.getValue(),
                                GameStatusEnum.ABANDONED.getValue())
                        .select(M2UserGameRecord::getGameId)
        );
        long kinds = dayRecords.stream().map(M2UserGameRecord::getGameId).distinct().count();
        daily.setGameCount((int) kinds);

        daily.setBalanceLevel(calcBalanceLevel(daily));

        dailyTrainMapper.upsertDailyTrain(daily);
    }

    private void applyDomainScore(M2UserDailyTrain daily, String cognitiveType, Integer passRate) {
        if (passRate == null) {
            return;
        }
        CognitiveDomainEnum domain = CognitiveDomainEnum.fromCode(cognitiveType);
        if (domain == null) {
            return;
        }
        switch (domain) {
            case MEMORY -> daily.setMemoryScore(mergeScore(daily.getMemoryScore(), passRate));
            case ATTENTION -> daily.setAttentionScore(mergeScore(daily.getAttentionScore(), passRate));
            case REASON -> daily.setReasonScore(mergeScore(daily.getReasonScore(), passRate));
            case LANGUAGE -> daily.setLanguageScore(mergeScore(daily.getLanguageScore(), passRate));
        }
    }

    private int mergeScore(Integer oldScore, int newScore) {
        if (oldScore == null || oldScore == 0) {
            return newScore;
        }
        return (oldScore + newScore) / 2;
    }

    private int calcBalanceLevel(M2UserDailyTrain d) {
        int[] scores = {
                nullToZero(d.getMemoryScore()),
                nullToZero(d.getAttentionScore()),
                nullToZero(d.getReasonScore()),
                nullToZero(d.getLanguageScore())
        };
        int trained = 0;
        int sum = 0;
        for (int s : scores) {
            if (s > 0) {
                trained++;
                sum += s;
            }
        }
        if (trained == 0) {
            return 0;
        }
        int coverage = trained * 25;
        double avg = sum * 1.0 / trained;
        double var = 0;
        for (int s : scores) {
            if (s > 0) {
                var += (s - avg) * (s - avg);
            }
        }
        var /= trained;
        double std = Math.sqrt(var);
        int stability = (int) Math.max(0, 50 - std);
        return Math.min(100, coverage / 2 + stability);
    }

    private String buildBalanceTip(DailyTrainVO vo) {
        int trained = 0;
        if (vo.getMemoryScore() > 0) trained++;
        if (vo.getAttentionScore() > 0) trained++;
        if (vo.getReasonScore() > 0) trained++;
        if (vo.getLanguageScore() > 0) trained++;

        if (trained == 0) {
            return "今天还没有训练，动动脑会更开心哦";
        }
        if (trained >= 3) {
            return "今天训练很均衡，大脑各个角落都活动到了";
        }
        if (vo.getMemoryScore() > 0 && vo.getAttentionScore() == 0
                && vo.getReasonScore() == 0 && vo.getLanguageScore() == 0) {
            return "今天记忆练得比较多，明天可以试试注意力小游戏";
        }
        return "再试一种别的小游戏，训练会更全面";
    }

    // ==================== 成就 ====================

    private Long createAchievement(String userId, M2UserGameRecord record, M2GameInfo game, int star) {
        M2UserGameAchievement a = new M2UserGameAchievement();
        a.setUserId(userId);
        a.setRecordId(record.getId());
        a.setGameId(game.getId());
        a.setStar(star);
        a.setAchievementDesc(String.format("完成「%s」获得 %d 星", game.getGameName(), star));
        a.setShareStatus(M2Constants.SHARE_NOT);
        achievementMapper.insert(a);
        return a.getId();
    }

    // ==================== 查询辅助 ====================

    private int resolveRecommendLevelNo(String userId, Long gameId) {
        M2UserGameRecord latest = recordMapper.selectLatestFinished(userId, gameId);
        if (latest != null && latest.getNextLevelNo() != null) {
            return latest.getNextLevelNo();
        }
        return M2Constants.MIN_LEVEL_NO;
    }

    private M2GameInfo requireEnabledGame(Long gameId) {
        M2GameInfo game = gameInfoMapper.selectById(gameId);
        if (game == null || !Objects.equals(game.getIsEnable(), M2Constants.ENABLE)) {
            throw new IllegalArgumentException("游戏不存在或已下架");
        }
        return game;
    }

    private M2GameLevel requireLevel(Long gameId, int levelNo) {
        M2GameLevel level = findLevel(gameId, levelNo);
        if (level == null) {
            level = gameLevelMapper.selectOne(
                    new LambdaQueryWrapper<M2GameLevel>()
                            .eq(M2GameLevel::getGameId, gameId)
                            .eq(M2GameLevel::getIsEnable, M2Constants.ENABLE)
                            .orderByAsc(M2GameLevel::getLevelNo)
                            .last("LIMIT 1")
            );
        }
        if (level == null) {
            throw new IllegalArgumentException("该游戏暂无可用难度配置");
        }
        return level;
    }

    private M2GameLevel requireLevelById(Long levelId) {
        M2GameLevel level = gameLevelMapper.selectById(levelId);
        if (level == null) {
            throw new IllegalArgumentException("难度配置不存在");
        }
        return level;
    }

    private M2GameLevel findLevel(Long gameId, int levelNo) {
        return gameLevelMapper.selectOne(
                new LambdaQueryWrapper<M2GameLevel>()
                        .eq(M2GameLevel::getGameId, gameId)
                        .eq(M2GameLevel::getLevelNo, levelNo)
                        .eq(M2GameLevel::getIsEnable, M2Constants.ENABLE)
                        .last("LIMIT 1")
        );
    }

    private int maxEnabledLevelNo(Long gameId) {
        List<M2GameLevel> levels = gameLevelMapper.selectList(
                new LambdaQueryWrapper<M2GameLevel>()
                        .eq(M2GameLevel::getGameId, gameId)
                        .eq(M2GameLevel::getIsEnable, M2Constants.ENABLE)
                        .orderByDesc(M2GameLevel::getLevelNo)
                        .last("LIMIT 1")
        );
        if (CollUtil.isEmpty(levels)) {
            return M2Constants.MIN_LEVEL_NO;
        }
        return levels.get(0).getLevelNo();
    }

    private int nullToZero(Integer v) {
        return v == null ? 0 : v;
    }
}
