package com.mogakjak.mogakjak.global.websocket.service;

import com.mogakjak.mogakjak.domain.lounge.dto.OfficialLoungeMemberResponse;
import com.mogakjak.mogakjak.global.websocket.dto.OfficialLoungePresenceUpdateDto;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessageSendingOperations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisPubSubServiceTest {

    @Test
    void npcPresenceSurvivesRedisJsonAndRoutesToExistingLoungeTopic() throws Exception {
        RedisPubSubService service = new RedisPubSubService(stringRedisTemplate, messageTemplate);
        var npc = new com.mogakjak.mogakjak.domain.lounge.dto.LoungeNpcMemberResponse(
                "A", com.mogakjak.mogakjak.domain.lounge.npc.LoungeNpcType.A, "모각이", null, 1,
                com.mogakjak.mogakjak.domain.user.entity.GroupParticipationStatus.PARTICIPATING,
                java.time.LocalDateTime.of(2026, 11, 4, 18, 0), 30L, "코딩테스트 준비");
        var payload = OfficialLoungePresenceUpdateDto.builder().eventType("NPC_UPDATE")
                .currentMemberCount(0L).displayMemberCount(1L).maxMemberCount(10)
                .members(List.of()).npcMembers(List.of(npc)).build();
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper()
                .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        when(message.getBody()).thenReturn(mapper.writeValueAsBytes(payload));
        service.onMessage(message, "official-lounge-presence".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        ArgumentCaptor<Object> captured = ArgumentCaptor.forClass(Object.class);
        verify(messageTemplate).convertAndSend(org.mockito.ArgumentMatchers.eq("/topic/lounge/presence"), captured.capture());
        var routed = assertInstanceOf(OfficialLoungePresenceUpdateDto.class, captured.getValue());
        assertEquals(List.of(npc), routed.getNpcMembers());
        assertEquals(0L, routed.getCurrentMemberCount());
        assertEquals(1L, routed.getDisplayMemberCount());
        assertEquals(List.of(), routed.getMembers());
    }

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private SimpMessageSendingOperations messageTemplate;

    @Mock
    private Message message;

    @Test
    void onMessage_routesOfficialLoungePresenceToLoungeTopic() throws Exception {
        RedisPubSubService service = new RedisPubSubService(stringRedisTemplate, messageTemplate);

        UUID loungeId = UUID.randomUUID();
        UUID changedUserId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();

        OfficialLoungePresenceUpdateDto payload = OfficialLoungePresenceUpdateDto.builder()
                .loungeId(loungeId)
                .eventType("ENTER")
                .changedUserId(changedUserId)
                .currentMemberCount(1L)
                .maxMemberCount(20)
                .members(List.of(
                        OfficialLoungeMemberResponse.builder()
                                .userId(memberId)
                                .nickname("kim")
                                .profileUrl("https://img.example.com/me.png")
                                .level(1)
                                .build()
                ))
                .build();

        String payloadJson = """
                {
                  "loungeId": "%s",
                  "eventType": "ENTER",
                  "changedUserId": "%s",
                  "currentMemberCount": 1,
                  "maxMemberCount": 20,
                  "members": [
                    {
                      "userId": "%s",
                      "nickname": "kim",
                      "profileUrl": "https://img.example.com/me.png",
                      "level": 1
                    }
                  ]
                }
                """.formatted(loungeId, changedUserId, memberId);

        when(message.getBody()).thenReturn(payloadJson.getBytes());

        service.onMessage(message, "official-lounge-presence".getBytes());

        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(messageTemplate).convertAndSend(org.mockito.ArgumentMatchers.eq("/topic/lounge/presence"), payloadCaptor.capture());
        Object routedPayload = payloadCaptor.getValue();

        assertInstanceOf(OfficialLoungePresenceUpdateDto.class, routedPayload);
        OfficialLoungePresenceUpdateDto routedDto = (OfficialLoungePresenceUpdateDto) routedPayload;
        assertEquals(loungeId, routedDto.getLoungeId());
        assertEquals("ENTER", routedDto.getEventType());
        assertEquals(changedUserId, routedDto.getChangedUserId());
        assertEquals(1L, routedDto.getCurrentMemberCount());
        assertEquals(20, routedDto.getMaxMemberCount());
        assertEquals(1, routedDto.getMembers().size());
        assertEquals(memberId, routedDto.getMembers().get(0).getUserId());
    }
}
