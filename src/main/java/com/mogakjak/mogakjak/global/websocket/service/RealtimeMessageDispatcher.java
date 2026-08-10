package com.mogakjak.mogakjak.global.websocket.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mogakjak.mogakjak.global.websocket.dto.ChatMessageDto;
import com.mogakjak.mogakjak.global.websocket.dto.CheerNotificationDto;
import com.mogakjak.mogakjak.global.websocket.dto.FocusNotificationDto;
import com.mogakjak.mogakjak.global.websocket.dto.FocusNotificationPublishDto;
import com.mogakjak.mogakjak.global.websocket.dto.GroupMemberStatusUpdateDto;
import com.mogakjak.mogakjak.global.websocket.dto.GroupTimerEventDto;
import com.mogakjak.mogakjak.global.websocket.dto.InvitationNotificationDto;
import com.mogakjak.mogakjak.global.websocket.dto.InvitationResponseNotificationDto;
import com.mogakjak.mogakjak.global.websocket.dto.OfficialLoungePresenceUpdateDto;
import com.mogakjak.mogakjak.global.websocket.dto.PokeNotificationDto;
import com.mogakjak.mogakjak.global.websocket.dto.TimerCompletionNotificationDto;
import com.mogakjak.mogakjak.global.websocket.dto.UserActiveStatusDto;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RealtimeMessageDispatcher {

    private final SimpMessageSendingOperations messageTemplate;
    private final ObjectMapper objectMapper;
    private final RealtimeMetrics realtimeMetrics;

    public void dispatch(String transport, String channel, String payload) {
        try {
            int deliveryCount = switch (channel) {
                case "chat" -> sendChat(payload);
                case "focus-notification" -> sendFocusNotification(payload);
                case "group-member-status" -> sendGroupMemberStatus(payload);
                case "official-lounge-presence" -> sendOfficialLoungePresence(payload);
                case "timer-completion" -> sendTimerCompletion(payload);
                case "poke-notification" -> sendPokeNotification(payload);
                case "cheer-notification" -> sendCheerNotification(payload);
                case "invitation-notification" -> sendInvitationNotification(payload);
                case "invitation-response" -> sendInvitationResponse(payload);
                case "group-timer-event" -> sendGroupTimerEvent(payload);
                case "user-active-status" -> sendUserActiveStatus(payload);
                default -> {
                    log.warn("지원하지 않는 실시간 채널입니다. channel={}", channel);
                    yield 0;
                }
            };

            realtimeMetrics.recordReceived(transport, channel);
            realtimeMetrics.recordDeliveries(transport, channel, deliveryCount);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("실시간 메시지 역직렬화에 실패했습니다. channel=" + channel, exception);
        }
    }

    private int sendChat(String payload) throws JsonProcessingException {
        ChatMessageDto dto = objectMapper.readValue(payload, ChatMessageDto.class);
        messageTemplate.convertAndSend("/topic/" + dto.getRoomId(), dto);
        return 1;
    }

    private int sendFocusNotification(String payload) throws JsonProcessingException {
        FocusNotificationPublishDto publishDto = objectMapper.readValue(payload, FocusNotificationPublishDto.class);
        FocusNotificationDto notification = publishDto.getNotification();
        List<UUID> recipientUserIds = publishDto.getRecipientUserIds();
        if (recipientUserIds == null || recipientUserIds.isEmpty()) {
            return 0;
        }

        for (UUID userId : recipientUserIds) {
            messageTemplate.convertAndSend("/topic/user/" + userId + "/focus-notification", notification);
        }
        return recipientUserIds.size();
    }

    private int sendGroupMemberStatus(String payload) throws JsonProcessingException {
        GroupMemberStatusUpdateDto dto = objectMapper.readValue(payload, GroupMemberStatusUpdateDto.class);
        messageTemplate.convertAndSend("/topic/group/" + dto.getGroupId() + "/member-status", dto);
        return 1;
    }

    private int sendOfficialLoungePresence(String payload) throws JsonProcessingException {
        OfficialLoungePresenceUpdateDto dto = objectMapper.readValue(payload, OfficialLoungePresenceUpdateDto.class);
        messageTemplate.convertAndSend("/topic/lounge/presence", dto);
        return 1;
    }

    private int sendTimerCompletion(String payload) throws JsonProcessingException {
        TimerCompletionNotificationDto dto = objectMapper.readValue(payload, TimerCompletionNotificationDto.class);
        messageTemplate.convertAndSend("/topic/user/" + dto.getUserId() + "/timer-completion", dto);
        return 1;
    }

    private int sendPokeNotification(String payload) throws JsonProcessingException {
        PokeNotificationDto dto = objectMapper.readValue(payload, PokeNotificationDto.class);
        messageTemplate.convertAndSend("/topic/user/" + dto.getTargetUserId() + "/poke", dto);
        return 1;
    }

    private int sendCheerNotification(String payload) throws JsonProcessingException {
        CheerNotificationDto dto = objectMapper.readValue(payload, CheerNotificationDto.class);
        messageTemplate.convertAndSend("/topic/user/" + dto.getTargetUserId() + "/cheer", dto);
        return 1;
    }

    private int sendInvitationNotification(String payload) throws JsonProcessingException {
        InvitationNotificationDto dto = objectMapper.readValue(payload, InvitationNotificationDto.class);
        messageTemplate.convertAndSend("/topic/user/" + dto.getInviteeId() + "/invitation", dto);
        return 1;
    }

    private int sendInvitationResponse(String payload) throws JsonProcessingException {
        InvitationResponseNotificationDto dto = objectMapper.readValue(payload, InvitationResponseNotificationDto.class);
        messageTemplate.convertAndSend("/topic/user/" + dto.getInviterId() + "/invitation-response", dto);
        return 1;
    }

    private int sendGroupTimerEvent(String payload) throws JsonProcessingException {
        GroupTimerEventDto dto = objectMapper.readValue(payload, GroupTimerEventDto.class);
        messageTemplate.convertAndSend("/topic/group/" + dto.getGroupId() + "/timer", dto);
        return 1;
    }

    private int sendUserActiveStatus(String payload) throws JsonProcessingException {
        UserActiveStatusDto dto = objectMapper.readValue(payload, UserActiveStatusDto.class);
        messageTemplate.convertAndSend("/topic/mates/active-status", dto);
        return 1;
    }
}
