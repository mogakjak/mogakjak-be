package com.mogakjak.mogakjak.domain.lounge;

import com.mogakjak.mogakjak.domain.group.entity.Group;
import com.mogakjak.mogakjak.domain.group.repository.GroupRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class OfficialLoungeBootstrap implements ApplicationRunner {

    private static final int OFFICIAL_LOUNGE_MAX_MEMBER_COUNT = 20;
    private static final String OFFICIAL_LOUNGE_NAME = "모각작 공식 라운지";
    private static final String OFFICIAL_LOUNGE_DESCRIPTION = "시스템이 운영하는 공용 광장";

    private final GroupRepository groupRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        groupRepository.findFirstByIsOfficialLoungeTrue()
                .ifPresentOrElse(
                        this::ensureOfficialLoungeConfig,
                        this::createOfficialLounge
                );
    }

    private void createOfficialLounge() {
        Group lounge = Group.builder()
                .name(OFFICIAL_LOUNGE_NAME)
                .imageUrl(null)
                .description(OFFICIAL_LOUNGE_DESCRIPTION)
                .password(null)
                .build();
        lounge.markAsOfficialLounge(OFFICIAL_LOUNGE_MAX_MEMBER_COUNT);
        groupRepository.save(lounge);
        log.info("공식 라운지 생성 완료: groupId={}", lounge.getId());
    }

    private void ensureOfficialLoungeConfig(Group lounge) {
        boolean changed = false;

        if (!OFFICIAL_LOUNGE_NAME.equals(lounge.getName())) {
            lounge.updateName(OFFICIAL_LOUNGE_NAME);
            changed = true;
        }

        if (lounge.getMaxMemberCount() == null || lounge.getMaxMemberCount() != OFFICIAL_LOUNGE_MAX_MEMBER_COUNT) {
            lounge.markAsOfficialLounge(OFFICIAL_LOUNGE_MAX_MEMBER_COUNT);
            changed = true;
        } else if (lounge.getIsOfficialLounge() == null || !lounge.getIsOfficialLounge()) {
            lounge.markAsOfficialLounge(OFFICIAL_LOUNGE_MAX_MEMBER_COUNT);
            changed = true;
        }

        if (changed) {
            groupRepository.save(lounge);
            log.info("공식 라운지 설정 보정 완료: groupId={}", lounge.getId());
        }
    }
}
