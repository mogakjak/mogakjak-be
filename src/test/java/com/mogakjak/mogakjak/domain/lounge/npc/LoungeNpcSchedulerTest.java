package com.mogakjak.mogakjak.domain.lounge.npc;

import com.mogakjak.mogakjak.domain.group.entity.Group;
import com.mogakjak.mogakjak.domain.group.repository.GroupRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Clock;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LoungeNpcSchedulerTest {
    private final LoungeNpcService service = mock(LoungeNpcService.class);
    private final GroupRepository groups = mock(GroupRepository.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final Clock clock = mock(Clock.class);
    private final LoungeNpcScheduler scheduler = new LoungeNpcScheduler(service, groups, events, clock);

    @BeforeEach
    void setUp() {
        Group lounge = Group.builder().name("공식 라운지").build();
        lounge.markAsOfficialLounge(10);
        when(groups.findFirstByIsOfficialLoungeTrue()).thenReturn(Optional.of(lounge));
    }

    @Test
    void publishesTransitionsImmediatelyAndHeartbeatEveryTenSeconds() {
        when(clock.millis()).thenReturn(0L, 9_999L, 10_000L, 11_000L);
        when(service.reconcile(10)).thenReturn(false, false, false, true);
        scheduler.tick();
        scheduler.tick();
        verify(events, times(1)).publishEvent(any(LoungeNpcUpdateEvent.class));
        scheduler.tick();
        scheduler.tick();
        verify(events, times(3)).publishEvent(any(LoungeNpcUpdateEvent.class));
    }

    @Test
    void aFailedTickDoesNotPreventNextTick() {
        when(service.reconcile(10)).thenThrow(new IllegalStateException("redis unavailable")).thenReturn(true);
        assertDoesNotThrow(scheduler::tick);
        scheduler.tick();
        verify(events).publishEvent(any(LoungeNpcUpdateEvent.class));
    }

    @Test
    void missingLoungeDoesNotCreateState() {
        when(groups.findFirstByIsOfficialLoungeTrue()).thenReturn(Optional.empty());
        scheduler.tick();
        verifyNoInteractions(service, events);
    }

    @Test
    void schedulerBeanIsAbsentUnlessExplicitlyEnabled() {
        ApplicationContextRunner runner = new ApplicationContextRunner()
                .withBean(LoungeNpcService.class, () -> service)
                .withBean(GroupRepository.class, () -> groups)
                .withBean(Clock.class, () -> clock)
                .withUserConfiguration(LoungeNpcScheduler.class);
        runner.run(context -> assertTrue(context.getBeansOfType(LoungeNpcScheduler.class).isEmpty()));
        runner.withPropertyValues("lounge.npc.enabled=true")
                .run(context -> assertEquals(1, context.getBeansOfType(LoungeNpcScheduler.class).size()));
    }
}
