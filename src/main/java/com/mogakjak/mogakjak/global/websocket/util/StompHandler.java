package com.mogakjak.mogakjak.global.websocket.util;

import com.mogakjak.mogakjak.global.websocket.service.ChatService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
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
import java.util.UUID;

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
            System.out.println("connect요청시 토큰 유효성 검증");
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

            System.out.println("토큰 검증 완료");
        }
        if(StompCommand.SUBSCRIBE == accessor.getCommand()){
            System.out.println("subscribe 검증");
            String destination = accessor.getDestination();
            System.out.println("구독 destination: " + destination);
            
            // 집중 체크 알림 구독인지 확인
            if (destination != null && destination.startsWith("/topic/group/") && destination.endsWith("/notification")) {
                // 집중 체크 알림 구독은 그룹 멤버 검증만 수행 (별도 처리)
                System.out.println("집중 체크 알림 구독 허용: " + destination);
                // 토큰 검증은 CONNECT에서 이미 수행되었으므로 여기서는 그룹 멤버 검증만 필요
                // 실제 그룹 멤버 검증은 필요시 추가 가능
                // 일단 허용
            } else if (destination != null && destination.startsWith("/topic/") && !destination.contains("/group/")) {
                // 채팅 룸 구독 검증 (/topic/{roomId} 패턴, /topic/group/으로 시작하지 않는 경우)
                System.out.println("채팅 룸 구독 검증: " + destination);
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
                        System.out.println("잘못된 roomId 형식: " + parts[2]);
                        throw new AuthenticationServiceException("잘못된 room ID 형식입니다.");
                    }
                }
            } else {
                System.out.println("알 수 없는 구독 패턴 또는 허용된 패턴: " + destination);
            }
        }

        return message;
        } catch (Exception e) {
            System.err.println("StompHandler preSend 에러: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

}
