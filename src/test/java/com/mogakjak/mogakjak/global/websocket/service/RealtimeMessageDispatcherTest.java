package com.mogakjak.mogakjak.global.websocket.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mogakjak.mogakjak.domain.lounge.dto.OfficialLoungeMemberResponse;
import com.mogakjak.mogakjak.global.websocket.dto.OfficialLoungePresenceUpdateDto;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessageSendingOperations;

@ExtendWith(MockitoExtension.class)
class RealtimeMessageDispatcherTest {

    @Mock
    private SimpMessageSendingOperations messageTemplate;

    @Mock
    private RealtimeMetrics realtimeMetrics;

    @Test
    void dispatch_routesOfficialLoungePresenceToLoungeTopic() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        RealtimeMessageDispatcher dispatcher =
                new RealtimeMessageDispatcher(messageTemplate, objectMapper, realtimeMetrics);

        UUID loungeId = UUID.randomUUID();
        UUID changedUserId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        OfficialLoungePresenceUpdateDto payload = OfficialLoungePresenceUpdateDto.builder()
                .loungeId(loungeId)
                .eventType("ENTER")
                .changedUserId(changedUserId)
                .currentMemberCount(1L)
                .maxMemberCount(20)
                .members(List.of(OfficialLoungeMemberResponse.builder().userId(memberId).nickname("kim").build()))
                .build();

        dispatcher.dispatch("local", "official-lounge-presence", objectMapper.writeValueAsString(payload));

        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(messageTemplate).convertAndSend(eq("/topic/lounge/presence"), payloadCaptor.capture());
        OfficialLoungePresenceUpdateDto routedDto =
                assertInstanceOf(OfficialLoungePresenceUpdateDto.class, payloadCaptor.getValue());
        assertEquals(loungeId, routedDto.getLoungeId());
        assertEquals(changedUserId, routedDto.getChangedUserId());
        assertEquals(memberId, routedDto.getMembers().getFirst().getUserId());
        verify(realtimeMetrics).recordReceived("local", "official-lounge-presence");
        verify(realtimeMetrics).recordDeliveries("local", "official-lounge-presence", 1);
    }
}
