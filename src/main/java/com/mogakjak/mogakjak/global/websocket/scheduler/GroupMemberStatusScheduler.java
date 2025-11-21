package com.mogakjak.mogakjak.global.websocket.scheduler;

import com.mogakjak.mogakjak.domain.group.entity.Group;
import com.mogakjak.mogakjak.domain.group.repository.GroupRepository;
import com.mogakjak.mogakjak.global.websocket.service.GroupMemberStatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 그룹 멤버 상태를 주기적으로 브로드캐스트하는 스케줄러
 * 개인 타이머 시간이 실시간으로 업데이트되도록 함
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GroupMemberStatusScheduler {

    private final GroupRepository groupRepository;
    private final GroupMemberStatusService groupMemberStatusService;

    /**
     * 10초마다 실행하여 모든 그룹의 멤버 상태를 브로드캐스트
     * 개인 타이머 시간이 실시간으로 업데이트되도록 함
     */
    @Scheduled(fixedRate = 10000) // 10초 = 10000ms
    @Transactional
    public void broadcastAllGroupMemberStatuses() {
        log.debug("그룹 멤버 상태 브로드캐스트 스케줄러 실행");
        
        List<Group> groups = groupRepository.findAll();

        for (Group group : groups) {
            try {
                // 그룹의 모든 멤버 상태 브로드캐스트
                groupMemberStatusService.broadcastAllMemberStatuses(group.getId());
            } catch (Exception e) {
                log.error("그룹 {} 멤버 상태 브로드캐스트 실패: {}", group.getId(), e.getMessage());
            }
        }
    }
}

