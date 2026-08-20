package com.xiaoliao.api.m3.service;

import com.xiaoliao.api.m3.dto.CollectRequest;
import com.xiaoliao.api.m3.dto.GoodThingSubmitRequest;
import com.xiaoliao.api.m3.dto.GratitudeSaveRequest;
import com.xiaoliao.api.m3.vo.AchievementVO;
import com.xiaoliao.api.m3.vo.GoodThingMonthVO;
import com.xiaoliao.api.m3.vo.GoodThingSubmitVO;
import com.xiaoliao.api.m3.vo.GoodThingTodayVO;
import com.xiaoliao.api.m3.vo.GratitudeVO;
import com.xiaoliao.api.m3.vo.WeeklySummaryVO;

import java.util.List;
import java.util.Map;

/**
 * M3 积极心理练习服务（不含正念冥想）
 * 三件好事 + 感恩留言 + 成就墙 + 每周开心小结
 */
public interface M3Service {

    /** 提交一件好事（每天最多 3 件） */
    GoodThingSubmitVO submitGoodThing(String userId, GoodThingSubmitRequest request);

    /** 今日已记录的好事 */
    GoodThingTodayVO todayGoodThings(String userId);

    /** 某月好事记录 */
    GoodThingMonthVO monthGoodThings(String userId, String month);

    /** 删除一条好事（仅限当天） */
    void deleteGoodThing(String userId, Long id);

    /** 保存感恩留言 */
    GratitudeVO saveGratitude(String userId, GratitudeSaveRequest request);

    /** 我的感恩留言列表 */
    List<GratitudeVO> listGratitude(String userId);

    /** 每周开心小结（模板生成） */
    WeeklySummaryVO weeklySummary(String userId);

    /** 成就墙（游戏成就 + 动态里程碑） */
    List<AchievementVO> achievements(String userId);

    /** 收藏/取消收藏成就 */
    Map<String, Object> collect(String userId, CollectRequest request);
}