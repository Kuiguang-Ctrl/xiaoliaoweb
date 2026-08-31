package com.xiaoliao.api.checkin;

import com.xiaoliao.api.checkin.dto.CheckinRequest;
import com.xiaoliao.api.checkin.dto.CheckinResultVO;
import com.xiaoliao.api.checkin.entity.CheckinRecord;
import com.xiaoliao.api.checkin.repository.CheckinMapper;
import com.xiaoliao.api.user.UserService;
import com.xiaoliao.api.user.entity.User;
import com.xiaoliao.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * CheckinService 单元测试 —— 业务规则验证（Mockito mock 掉 Mapper，不依赖数据库）。
 * <p>
 * 覆盖：用户校验、mood 校验、一天一签幂等（409）、首次签到、连续负面情绪关怀提醒、月历参数校验。
 */
@ExtendWith(MockitoExtension.class)
class CheckinServiceTest {

    @Mock
    private CheckinMapper checkinMapper;

    @Mock
    private UserService userService;

    @InjectMocks
    private CheckinService checkinService;

    @Test
    void checkIn_用户不存在时返回401且不落库() {
        when(userService.getById("u-1")).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> checkinService.checkIn("u-1", new CheckinRequest()));

        assertEquals(401, ex.getCode());
        verify(checkinMapper, never()).insert(any());
    }

    @Test
    void checkIn_mood非法时返回400() {
        when(userService.getById("u-1")).thenReturn(new User());
        CheckinRequest request = new CheckinRequest();
        request.setMood("angry");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> checkinService.checkIn("u-1", request));

        assertEquals(400, ex.getCode());
    }

    @Test
    void checkIn_当天已签到返回409幂等拦截() {
        when(userService.getById("u-1")).thenReturn(new User());
        when(checkinMapper.selectOne(any())).thenReturn(new CheckinRecord());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> checkinService.checkIn("u-1", new CheckinRequest()));

        assertEquals(409, ex.getCode());
        verify(checkinMapper, never()).insert(any());
    }

    @Test
    void checkIn_首次签到成功且无负面关怀() {
        when(userService.getById("u-1")).thenReturn(new User());
        when(checkinMapper.selectOne(any())).thenReturn(null);
        when(checkinMapper.selectList(any())).thenReturn(new ArrayList<>());

        CheckinResultVO result = checkinService.checkIn("u-1", request("sunny"));

        assertNotNull(result);
        assertEquals("sunny", result.getMood());
        assertFalse(result.isCareAlert());
        verify(checkinMapper).insert(any(CheckinRecord.class));
    }

    @Test
    void checkIn_连续3天负面情绪触发关怀提醒() {
        when(userService.getById("u-1")).thenReturn(new User());
        when(checkinMapper.selectOne(any())).thenReturn(null);
        when(checkinMapper.selectList(any())).thenReturn(rainyDays(3));

        CheckinResultVO result = checkinService.checkIn("u-1", request("rain"));

        assertTrue(result.isCareAlert());
        assertEquals(3, result.getConsecutiveDays());
    }

    @Test
    void checkIn_负面情绪未满3天不触发关怀() {
        when(userService.getById("u-1")).thenReturn(new User());
        when(checkinMapper.selectOne(any())).thenReturn(null);
        when(checkinMapper.selectList(any())).thenReturn(rainyDays(2));

        CheckinResultVO result = checkinService.checkIn("u-1", request("rain"));

        assertFalse(result.isCareAlert());
        assertEquals(2, result.getConsecutiveDays());
    }

    @Test
    void calendar_月份格式非法时返回400() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> checkinService.calendar("u-1", "2026/08"));

        assertEquals(400, ex.getCode());
    }

    private CheckinRequest request(String mood) {
        CheckinRequest request = new CheckinRequest();
        request.setMood(mood);
        return request;
    }

    /** 构造截至今天连续 n 天的"rain"签到记录（时间倒序） */
    private List<CheckinRecord> rainyDays(int days) {
        LocalDate today = LocalDate.now();
        List<CheckinRecord> records = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            CheckinRecord record = new CheckinRecord();
            record.setMood("rain");
            record.setCheckinDate(today.minusDays(i));
            records.add(record);
        }
        return records;
    }
}
