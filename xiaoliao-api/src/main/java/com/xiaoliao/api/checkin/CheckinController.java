package com.xiaoliao.api.checkin;

import com.xiaoliao.api.config.AuthContext;
import com.xiaoliao.api.checkin.dto.CalendarVO;
import com.xiaoliao.api.checkin.dto.CheckinRequest;
import com.xiaoliao.api.checkin.dto.CheckinResultVO;
import com.xiaoliao.api.checkin.dto.TodayVO;
import com.xiaoliao.api.checkin.CheckinService;
import com.xiaoliao.api.metrics.ApiMetric;
import com.xiaoliao.common.dto.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * M1 每日签到与情绪天气
 */
@Tag(name = "M1 每日签到与情绪天气")
@RestController
@RequestMapping("/api/checkin")
@RequiredArgsConstructor
public class CheckinController {
    private final CheckinService checkinService;

    /**
     * 快捷签到：首页点情绪按钮即完成，可传 mood 也可空
     */
    @Operation(summary = "快捷签到", description = "小程序首页点情绪按钮即完成签到，mood 可空")
    @PostMapping
    @ApiMetric("checkin.check")
    public Result<CheckinResultVO> checkIn(@RequestAttribute(AuthContext.USER_ID_ATTR) String userId,
                                           @RequestBody CheckinRequest request) {
        return Result.ok("签到成功", checkinService.checkIn(userId, request));
    }

    /**
     * 今日状态：小程序首页加载
     */
    @Operation(summary = "今日状态", description = "小程序首页加载：日期/星期/季节/问候/是否已签/连续天数")
    @GetMapping("/today")
    @ApiMetric("checkin.today")
    public Result<TodayVO> today(@RequestAttribute(AuthContext.USER_ID_ATTR) String userId) {
        return Result.ok(checkinService.today(userId));
    }

    /**
     * 情绪月历：回看某月签到记录
     */
    @Operation(summary = "情绪月历", description = "回看某月的每日签到记录与月度统计，month 格式 yyyy-MM，默认当月")
    @GetMapping("/calendar")
    public Result<CalendarVO> calendar(@RequestAttribute(AuthContext.USER_ID_ATTR) String userId,
                                       @RequestParam(required = false) String month) {
        return Result.ok(checkinService.calendar(userId, month));
    }
}
