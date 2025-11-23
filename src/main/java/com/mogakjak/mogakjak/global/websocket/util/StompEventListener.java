package com.mogakjak.mogakjak.global.websocket.util;

import com.mogakjak.mogakjak.domain.timer.repository.ActiveFocusSessionRepository;
import com.mogakjak.mogakjak.domain.user.entity.User;
import com.mogakjak.mogakjak.domain.user.repository.UserRepository;
import com.mogakjak.mogakjak.global.auth.security.util.JwtUtil;
import com.mogakjak.mogakjak.global.websocket.service.UserActiveStatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

//스프링과 stomp는 기본적으로 세션관리를 자동(내부적)으로 처리
//연결/해제 이벤트를 기록, 연결된 세션수를 실시간으로 확인할 목적으로 이벤트 리스너를 생성 => 로그, 디버깅 목적
@Slf4j
@Component
@RequiredArgsConstructor
public class StompEventListener {

    private final Set<String> sessions = ConcurrentHashMap.newKeySet();
    private final UserRepository userRepository;
    private final ActiveFocusSessionRepository activeFocusSessionRepository;
    private final JwtUtil jwtUtil;
    private final UserActiveStatusService userActiveStatusService;

    @EventListener
    @Transactional
    public void connectHandle(SessionConnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        sessions.add(accessor.getSessionId());
        log.info("WebSocket 연결: sessionId={}, total sessions={}", accessor.getSessionId(), sessions.size());

        // 토큰에서 사용자 ID 추출하여 isActive 업데이트
        try {
            UUID userId = extractUserIdFromEvent(accessor);
            if (userId != null) {
                User user = userRepository.findById(userId).orElse(null);
                if (user != null) {
                    // 웹사이트 접속 중이므로 isActive를 true로 설정
                    // 단, 이미 활성 타이머가 있으면 그대로 유지 (이미 true일 수 있음)
                    boolean wasActive = user.getIsActive();
                    user.setActive(true);
                    userRepository.save(user);
                    log.debug("사용자 {}의 isActive를 true로 설정 (WebSocket 연결)", userId);
                    
                    // 상태가 변경되었으면 브로드캐스트 (트랜잭션 커밋 후)
                    if (!wasActive) {
                        TransactionSynchronizationManager.registerSynchronization(
                            new TransactionSynchronizationAdapter() {
                                @Override
                                public void afterCommit() {
                                    userActiveStatusService.broadcastActiveStatusChange(userId, true);
                                }
                            }
                        );
                    }
                }
            }
        } catch (Exception e) {
            log.warn("WebSocket 연결 시 isActive 업데이트 실패: {}", e.getMessage());
        }
    }

    @EventListener
    @Transactional
    public void disconnectHandle(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        sessions.remove(accessor.getSessionId());
        log.info("WebSocket 해제: sessionId={}, total sessions={}", accessor.getSessionId(), sessions.size());

        // 토큰에서 사용자 ID 추출하여 isActive 업데이트
        try {
            UUID userId = extractUserIdFromEvent(accessor);
            if (userId != null) {
                User user = userRepository.findById(userId).orElse(null);
                if (user != null) {
                    // 활성 타이머가 있는지 확인
                    boolean hasActiveTimer = activeFocusSessionRepository.findByUserId(userId).isPresent();
                    boolean wasActive = user.getIsActive();
                    boolean newActiveStatus;
                    
                    // 활성 타이머가 없으면 isActive를 false로 설정
                    // 활성 타이머가 있으면 true로 유지 (타이머가 실행 중이므로)
                    if (!hasActiveTimer) {
                        user.setActive(false);
                        newActiveStatus = false;
                        userRepository.save(user);
                        log.debug("사용자 {}의 isActive를 false로 설정 (WebSocket 해제, 활성 타이머 없음)", userId);
                    } else {
                        newActiveStatus = true;
                        log.debug("사용자 {}의 isActive를 true로 유지 (활성 타이머 있음)", userId);
                    }
                    
                    // 상태가 변경되었으면 브로드캐스트 (트랜잭션 커밋 후)
                    if (wasActive != newActiveStatus) {
                        TransactionSynchronizationManager.registerSynchronization(
                            new TransactionSynchronizationAdapter() {
                                @Override
                                public void afterCommit() {
                                    userActiveStatusService.broadcastActiveStatusChange(userId, newActiveStatus);
                                }
                            }
                        );
                    }
                }
            }
        } catch (Exception e) {
            log.warn("WebSocket 해제 시 isActive 업데이트 실패: {}", e.getMessage());
        }
    }

    /**
     * StompHeaderAccessor에서 토큰을 추출하여 사용자 ID를 반환
     */
    private UUID extractUserIdFromEvent(StompHeaderAccessor accessor) {
        try {
            String token = null;

            // 헤더에서 토큰 읽기
            String bearerToken = accessor.getFirstNativeHeader("Authorization");
            if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
                token = bearerToken.substring(7);
            } else {
                // 세션 속성에서 토큰 읽기 (핸드셰이크 인터셉터에서 설정한 토큰)
                Map<String, Object> sessionAttrs = accessor.getSessionAttributes();
                if (sessionAttrs != null) {
                    Object tokenObj = sessionAttrs.get("token");
                    if (tokenObj != null) {
                        token = tokenObj.toString();
                    }
                }
            }

            if (token == null || token.isEmpty()) {
                log.debug("토큰을 찾을 수 없습니다. sessionId={}", accessor.getSessionId());
                return null;
            }

            // JwtUtil을 사용하여 userId 추출
            String userIdStr = jwtUtil.getUserIdFromToken(token);
            if (userIdStr == null || userIdStr.isEmpty()) {
                log.debug("토큰에서 userId를 추출할 수 없습니다. sessionId={}", accessor.getSessionId());
                return null;
            }

            return UUID.fromString(userIdStr);
        } catch (Exception e) {
            log.warn("토큰에서 사용자 ID 추출 실패: {}", e.getMessage());
            return null;
        }
    }
}
