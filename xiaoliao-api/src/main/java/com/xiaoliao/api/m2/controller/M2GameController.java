package com.xiaoliao.api.m2.controller;

import com.xiaoliao.api.config.AuthContext;
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
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "M2 脑力小游戏")
@RestController
@RequestMapping("/api/v1/m2/games")
@RequiredArgsConstructor
public class M2GameController {

    private final M2GameService m2GameService;

    @Operation(summary = "游戏列表（含推荐难度）")
    @GetMapping
    public Result<List<GameListVO>> list(
            @RequestAttribute(AuthContext.USER_ID_ATTR) String userId) {
        List<GameListVO> list = m2GameService.listGames(userId);
        return Result.ok(list);
    }

    @Operation(summary = "开始一局")
    @PostMapping("/{gameId}/start")
    public Result<GameStartVO> start(
            @PathVariable Long gameId,
            @RequestAttribute(AuthContext.USER_ID_ATTR) String userId) {
        GameStartVO vo = m2GameService.start(userId, gameId);
        return Result.ok(vo);
    }

    @Operation(summary = "结束一局（触发自适应难度）")
    @PostMapping("/finish")
    public Result<GameFinishResultVO> finish(
            @RequestAttribute(AuthContext.USER_ID_ATTR) String userId,
            @Valid @RequestBody GameFinishVO body) {
        GameFinishResultVO result = m2GameService.finish(userId, body);
        return Result.ok(result);
    }

    @Operation(summary = "每日训练汇总")
    @GetMapping("/train/daily")
    public Result<DailyTrainVO> dailyTrain(
            @RequestAttribute(AuthContext.USER_ID_ATTR) String userId,
            @Parameter(description = "日期 yyyy-MM-dd，默认今天") @RequestParam(required = false) String date) {
        DailyTrainVO vo = m2GameService.dailyTrain(userId, date);
        return Result.ok(vo);
    }
}
