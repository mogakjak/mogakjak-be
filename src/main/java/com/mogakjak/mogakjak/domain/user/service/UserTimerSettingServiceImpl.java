package com.mogakjak.mogakjak.domain.user.service;

import com.mogakjak.mogakjak.domain.timer.enumerate.TimerMode;
import com.mogakjak.mogakjak.domain.user.controller.dto.TimerTabOrderRequest;
import com.mogakjak.mogakjak.domain.user.controller.dto.TimerTabOrderResponse;
import com.mogakjak.mogakjak.domain.user.entity.User;
import com.mogakjak.mogakjak.domain.user.entity.UserTimerSetting;
import com.mogakjak.mogakjak.domain.user.repository.UserTimerSettingRepository;
import com.mogakjak.mogakjak.global.exception.CustomException;
import com.mogakjak.mogakjak.global.exception.status.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserTimerSettingServiceImpl implements UserTimerSettingService {

    private final UserTimerSettingRepository userTimerSettingRepository;

    @Override
    public TimerTabOrderResponse getTimerTabOrder(User user) {
        UserTimerSetting setting = userTimerSettingRepository.findById(user.getId())
                .orElseGet(() -> UserTimerSetting.createDefault(user));
        return TimerTabOrderResponse.from(setting);
    }

    @Override
    @Transactional
    public void updateTimerTabOrder(User user, TimerTabOrderRequest request) {
        List<TimerMode> tabOrder = request.getTabOrder();
        if (tabOrder.size() != TimerMode.values().length || !tabOrder.containsAll(List.of(TimerMode.values()))) {
            throw new CustomException(ErrorCode.INVALID_TIMER_TAB_ORDER);
        }

        UserTimerSetting setting = userTimerSettingRepository.findById(user.getId())
                .orElseGet(() -> UserTimerSetting.createDefault(user));
        setting.updateTimerTabOrder(tabOrder);
        userTimerSettingRepository.save(setting);
    }
}