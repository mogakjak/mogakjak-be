package com.mogakjak.mogakjak.global.websocket.util;

import com.mogakjak.mogakjak.global.websocket.service.ChatService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
public class StompHandler implements ChannelInterceptor {

    @Value("${jwt.secret-key}")
    private String secretKey;
    private final ChatService chatService;

    public StompHandler(ChatService chatService) {
        this.chatService = chatService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        try {
            final StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        if(StompCommand.CONNECT == accessor.getCommand()){
            log.info("=== CONNECT 요청 토큰 유효성 검증 시작 ===");
            log.info("세션 ID: {}", accessor.getSessionId());
            
            // 모든 헤더 출력
            log.debug("모든 헤더:");
            accessor.toNativeHeaderMap().forEach((key, value) -> {
                log.debug("  {}: {}", key, value);
            });
            
            String bearerToken = accessor.getFirstNativeHeader("Authorization");
            String token = null;
            
            // 헤더에서 토큰 읽기
            if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
                token = bearerToken.substring(7);
                log.info("CONNECT: Authorization 헤더에서 토큰 발견 (길이: {})", token.length());
            } else {
                log.warn("CONNECT: Authorization 헤더가 없거나 Bearer 형식이 아닙니다. bearerToken={}", bearerToken);
                
                // 핸드셰이크 인터셉터에서 설정한 토큰 사용
                Map<String, Object> sessionAttrs = accessor.getSessionAttributes();
                if (sessionAttrs != null) {
                    log.debug("CONNECT: 세션 속성 키 목록: {}", sessionAttrs.keySet());
                    Object tokenObj = sessionAttrs.get("token");
                    if (tokenObj != null) {
                        token = tokenObj.toString();
                        log.info("CONNECT: 세션 속성에서 토큰 발견 (길이: {})", token.length());
                    } else {
                        log.warn("CONNECT: 세션 속성에 'token' 키가 없습니다.");
                    }
                } else {
                    log.warn("CONNECT: 세션 속성이 null입니다.");
                }
            }
            
            if (token == null || token.isEmpty()) {
                log.error("CONNECT: 토큰을 찾을 수 없습니다. Authorization 헤더와 세션 속성을 모두 확인했습니다.");
                log.error("CONNECT: 세션 ID: {}", accessor.getSessionId());
                throw new AuthenticationServiceException("토큰이 없습니다.");
            }
//            토큰 검증
//            Jwts.parserBuilder()
//                    .setSigningKey(secretKey)
//                    .build()
//                    .parseClaimsJws(token)
//                    .getBody();
            SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            log.info("토큰 검증 완료");
        }
        if(StompCommand.SUBSCRIBE == accessor.getCommand()){
            log.debug("subscribe 검증");
            String destination = accessor.getDestination();
            log.debug("구독 destination: {}", destination);
            
                   // 집중 체크 알림 구독인지 확인
                   if (destination != null && destination.startsWith("/topic/group/") && destination.endsWith("/notification")) {
                       // 집중 체크 알림 구독은 그룹 멤버 검증만 수행 (별도 처리)
                       log.info("집중 체크 알림 구독 허용: {}", destination);
                       // 토큰 검증은 CONNECT에서 이미 수행되었으므로 여기서는 그룹 멤버 검증만 필요
                       // 실제 그룹 멤버 검증은 필요시 추가 가능
                       // 일단 허용
                   } else if (destination != null && destination.startsWith("/topic/group/") && destination.endsWith("/member-status")) {
                       // 그룹 멤버 상태 구독 허용
                       log.info("그룹 멤버 상태 구독 허용: {}", destination);
                       // 토큰 검증은 CONNECT에서 이미 수행되었으므로 허용
                   } else if (destination != null && destination.startsWith("/topic/user/") && destination.endsWith("/timer-completion")) {
                       // 개인 타이머 완료 알림 구독 허용
                       log.info("타이머 완료 알림 구독 허용: {}", destination);
                       // 토큰 검증은 CONNECT에서 이미 수행되었으므로 허용
                   } else if (destination != null && destination.startsWith("/topic/user/") && destination.endsWith("/poke")) {
                       // 콕 찌르기 알림 구독 허용
                       log.info("콕 찌르기 알림 구독 허용: {}", destination);
                       // 토큰 검증은 CONNECT에서 이미 수행되었으므로 허용
                   } else if (destination != null && destination.startsWith("/topic/user/") && destination.endsWith("/cheer")) {
                       // 응원 알림 구독 허용
                       log.info("응원 알림 구독 허용: {}", destination);
                       // 토큰 검증은 CONNECT에서 이미 수행되었으므로 허용
                   } else if (destination != null && destination.startsWith("/topic/group/") && destination.endsWith("/timer")) {
                       // 그룹 타이머 이벤트 구독 허용
                       log.info("그룹 타이머 이벤트 구독 허용: {}", destination);
                       // 토큰 검증은 CONNECT에서 이미 수행되었으므로 허용
                   } else if (destination != null && destination.startsWith("/topic/") && !destination.contains("/group/") && !destination.contains("/user/")) {
                // 채팅 룸 구독 검증 (/topic/{roomId} 패턴, /topic/group/으로 시작하지 않는 경우)
                log.debug("채팅 룸 구독 검증: {}", destination);
                String bearerToken = accessor.getFirstNativeHeader("Authorization");
                String token = null;
                
                // 헤더에서 토큰 읽기
                if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
                    token = bearerToken.substring(7);
                } else {
                    // 핸드셰이크 인터셉터에서 설정한 토큰 사용
                    Object tokenObj = accessor.getSessionAttributes().get("token");
                    if (tokenObj != null) {
                        token = tokenObj.toString();
                    }
                }
                
                if (token == null || token.isEmpty()) {
                    throw new AuthenticationServiceException("토큰이 없습니다.");
                }

                SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
                Claims claims = Jwts.parser()
                        .verifyWith(key)
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();

                String email = claims.getSubject();
                String[] parts = destination.split("/");
                if (parts.length >= 3) {
                    try {
                        String roomId = parts[2];
                        UUID roomUuid = UUID.fromString(roomId);
                        if(!chatService.isRoomPaticipant(email, roomUuid)){
                            throw new AuthenticationServiceException("해당 room에 권한이 없습니다.");
                        }
                    } catch (IllegalArgumentException e) {
                        log.error("잘못된 roomId 형식: {}", parts[2]);
                        throw new AuthenticationServiceException("잘못된 room ID 형식입니다.");
                    }
                }
            } else {
                log.debug("알 수 없는 구독 패턴 또는 허용된 패턴: {}", destination);
            }
        }

        return message;
        } catch (Exception e) {
            log.error("StompHandler preSend 에러: {}", e.getMessage(), e);
            throw e;
        }
    }

}
