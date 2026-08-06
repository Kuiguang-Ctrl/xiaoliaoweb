package com.xiaoliao.api.m2.controller;

import com.xiaoliao.api.m2.service.M2GameService;
import com.xiaoliao.api.m2.vo.DailyTrainVO;
import com.xiaoliao.api.m2.vo.GameFinishResultVO;
import com.xiaoliao.api.m2.vo.GameFinishVO;
import com.xiaoliao.api.m2.vo.GameListVO;
import com.xiaoliao.api.m2.vo.GameStartVO;
import com.xiaoliao.common.dto.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "M2 脑力小游戏")
@RestController
@RequestMapping("/api/v1/m2/games")
@RequiredArgsConstructor
public class M2GameController {

    private final M2GameService m2GameService;

    private Long currentUserId(Long userIdParam) {
        if (userIdParam == null) {
            throw new IllegalArgumentException("userId 不能为空（联调阶段请传参，上线后从登录态获取）");
        }
        return userIdParam;
    }

    @Operation(summary = "游戏列表（含推荐难度）")
    @GetMapping
    public Result<List<GameListVO>> list(
            @Parameter(description = "用户ID，联调用") @RequestParam Long userId) {
        List<GameListVO> list = m2GameService.listGames(currentUserId(userId));
        return Result.ok(list);
    }

    @Operation(summary = "开始一局")
    @PostMapping("/{gameId}/start")
    public Result<GameStartVO> start(
            @PathVariable Long gameId,
            @Parameter(description = "用户ID，联调用") @RequestParam Long userId) {
        GameStartVO vo = m2GameService.start(currentUserId(userId), gameId);
        return Result.ok(vo);
    }

    @Operation(summary = "结束一局（触发自适应难度）")
    @PostMapping("/finish")
    public Result<GameFinishResultVO> finish(
            @Parameter(description = "用户ID，联调用") @RequestParam Long userId,
            @Valid @RequestBody GameFinishVO body) {
        GameFinishResultVO result = m2GameService.finish(currentUserId(userId), body);
        return Result.ok(result);
    }

    @Operation(summary = "每日训练汇总")
    @GetMapping("/train/daily")
    public Result<DailyTrainVO> dailyTrain(
            @Parameter(description = "用户ID，联调用") @RequestParam Long userId,
            @Parameter(description = "日期 yyyy-MM-dd，默认今天") @RequestParam(required = false) String date) {
        DailyTrainVO vo = m2GameService.dailyTrain(currentUserId(userId), date);
        return Result.ok(vo);
    }
}
