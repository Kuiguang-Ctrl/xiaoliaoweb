package com.xiaoliao.api.m3.controller;

import com.xiaoliao.api.config.AuthContext;
import com.xiaoliao.api.m3.dto.CollectRequest;
import com.xiaoliao.api.m3.dto.GoodThingSubmitRequest;
import com.xiaoliao.api.m3.dto.GratitudeSaveRequest;
import com.xiaoliao.api.m3.service.M3Service;
import com.xiaoliao.api.m3.vo.AchievementVO;
import com.xiaoliao.api.m3.vo.GoodThingMonthVO;
import com.xiaoliao.api.m3.vo.GoodThingSubmitVO;
import com.xiaoliao.api.m3.vo.GoodThingTodayVO;
import com.xiaoliao.api.m3.vo.GratitudeVO;
import com.xiaoliao.api.m3.vo.WeeklySummaryVO;
import com.xiaoliao.common.dto.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * M3 积极心理练习（不含正念冥想）
 * 三件好事 + 感恩留言 + 成就墙 + 每周开心小结
 */
@Tag(name = "M3 积极心理练习")
@RestController
@RequestMapping("/api/v1/m3")
@RequiredArgsConstructor
public class M3Controller {

    private final M3Service m3Service;

    @Operation(summary = "提交一件好事", description = "每天最多 3 件，返回今日条数与鼓励语")
    @PostMapping("/good-things")
    public Result<GoodThingSubmitVO> submitGoodThing(
            @Parameter(hidden = true) @RequestAttribute(AuthContext.USER_ID_ATTR) String userId,
            @Valid @RequestBody GoodThingSubmitRequest request) {
        return Result.ok("记录成功", m3Service.submitGoodThing(userId, request));
    }

    @Operation(summary = "今日三件好事", description = "今日已记录的好事列表与条数")
    @GetMapping("/good-things/today")
    public Result<GoodThingTodayVO> todayGoodThings(
            @Parameter(hidden = true) @RequestAttribute(AuthContext.USER_ID_ATTR) String userId) {
        return Result.ok(m3Service.todayGoodThings(userId));
    }

    @Operation(summary = "历史好事记录", description = "回看某月好事记录，month 格式 yyyy-MM，默认当月")
    @GetMapping("/good-things/records")
    public Result<GoodThingMonthVO> monthGoodThings(
            @Parameter(hidden = true) @RequestAttribute(AuthContext.USER_ID_ATTR) String userId,
            @RequestParam(required = false) String month) {
        return Result.ok(m3Service.monthGoodThings(userId, month));
    }

    @Operation(summary = "删除一条好事", description = "仅限当天记录，温和无惩罚")
    @DeleteMapping("/good-things/{id}")
    public Result<Void> deleteGoodThing(
            @Parameter(hidden = true) @RequestAttribute(AuthContext.USER_ID_ATTR) String userId,
            @PathVariable Long id) {
        m3Service.deleteGoodThing(userId, id);
        return Result.ok("删除成功", null);
    }

    @Operation(summary = "保存感恩留言", description = "给家人/老友写一段感谢话，返回可复制分享文案")
    @PostMapping("/gratitude")
    public Result<GratitudeVO> saveGratitude(
            @Parameter(hidden = true) @RequestAttribute(AuthContext.USER_ID_ATTR) String userId,
            @Valid @RequestBody GratitudeSaveRequest request) {
        return Result.ok("留言保存成功", m3Service.saveGratitude(userId, request));
    }

    @Operation(summary = "我的感恩留言", description = "按时间倒序返回最近 50 条")
    @GetMapping("/gratitude")
    public Result<List<GratitudeVO>> listGratitude(
            @Parameter(hidden = true) @RequestAttribute(AuthContext.USER_ID_ATTR) String userId) {
        return Result.ok(m3Service.listGratitude(userId));
    }

    @Operation(summary = "每周开心小结", description = "本周一至周日：好事/感恩/游戏/成就/签到统计与模板小结")
    @GetMapping("/weekly-summary")
    public Result<WeeklySummaryVO> weeklySummary(
            @Parameter(hidden = true) @RequestAttribute(AuthContext.USER_ID_ATTR) String userId) {
        return Result.ok(m3Service.weeklySummary(userId));
    }

    @Operation(summary = "成就墙", description = "游戏成就（复用M2）+ 动态里程碑（签到7天/好事10件/感恩3份），含收藏状态")
    @GetMapping("/achievements")
    public Result<List<AchievementVO>> achievements(
            @Parameter(hidden = true) @RequestAttribute(AuthContext.USER_ID_ATTR) String userId) {
        return Result.ok(m3Service.achievements(userId));
    }

    @Operation(summary = "收藏/取消收藏成就", description = "collect=true 收藏，false 取消收藏")
    @PostMapping("/achievements/collect")
    public Result<Map<String, Object>> collect(
            @Parameter(hidden = true) @RequestAttribute(AuthContext.USER_ID_ATTR) String userId,
            @Valid @RequestBody CollectRequest request) {
        return Result.ok(m3Service.collect(userId, request));
    }
}