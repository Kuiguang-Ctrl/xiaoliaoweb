package com.xiaoliao.api.m2.service;

import com.xiaoliao.api.m2.vo.DailyTrainVO;
import com.xiaoliao.api.m2.vo.GameFinishResultVO;
import com.xiaoliao.api.m2.vo.GameFinishVO;
import com.xiaoliao.api.m2.vo.GameListVO;
import com.xiaoliao.api.m2.vo.GameStartVO;

import java.util.List;

/**
 * M2 脑力游戏服务
 */
public interface M2GameService {

    /**
     * 启用中的游戏列表，并附带用户当前推荐难度
     */
    List<GameListVO> listGames(String userId);

    /**
     * 开始一局：按最近完成局的 next_level_no 选难度，创建进行中记录
     */
    GameStartVO start(String userId, Long gameId);

    /**
     * 结束一局：计算正确率与星级，自适应升降级，写日汇总与成就
     */
    GameFinishResultVO finish(String userId, GameFinishVO vo);

    /**
     * 查询某日训练汇总（默认今天）
     */
    DailyTrainVO dailyTrain(String userId, String dateStr);
}
