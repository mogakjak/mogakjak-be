package com.mogakjak.mogakjak.global.websocket.util;

import com.mogakjak.mogakjak.domain.timer.repository.ActiveFocusSessionRepository;
import com.mogakjak.mogakjak.domain.user.entity.User;
import com.mogakjak.mogakjak.domain.user.repository.UserRepository;
import com.mogakjak.mogakjak.global.auth.security.util.JwtUtil;
import com.mogakjak.mogakjak.domain.lounge.service.OfficialLoungePresenceService;
import com.mogakjak.mogakjak.domain.lounge.service.OfficialLoungeService;
import com.mogakjak.mogakjak.global.websocket.service.UserActiveStatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
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
    // 세션 ID와 userId 매핑 (해제 시 올바른 userId를 찾기 위해)
    private final Map<String, UUID> sessionUserIdMap = new ConcurrentHashMap<>();
    private final UserRepository userRepository;
    private final ActiveFocusSessionRepository activeFocusSessionRepository;
    private final JwtUtil jwtUtil;
    private final UserActiveStatusService userActiveStatusService;
    private final OfficialLoungePresenceService officialLoungePresenceService;
    private final OfficialLoungeService officialLoungeService;

    @EventListener
    @Transactional
    public void connectHandle(SessionConnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        sessions.add(accessor.getSessionId());
        log.info("===== WebSocket 연결 이벤트 ===== sessionId={}, total sessions={}", accessor.getSessionId(), sessions.size());

        // 토큰에서 사용자 ID 추출하여 isActive 업데이트
        try {
            String sessionId = accessor.getSessionId();
            UUID userId = extractUserIdFromEvent(accessor);
            log.info("WebSocket 연결: sessionId={}, 추출된 userId={}", sessionId, userId);
            if (userId != null) {
                // 세션 ID와 userId 매핑 저장
                sessionUserIdMap.put(sessionId, userId);
                log.info("WebSocket 연결: 세션 매핑 저장: sessionId={}, userId={}", sessionId, userId);
                long sessionCount = officialLoungePresenceService.registerWebSocketSession(userId);
                log.info("공식 라운지 웹소켓 세션 등록: userId={}, sessionCount={}", userId, sessionCount);
                
                User user = userRepository.findById(userId).orElse(null);
                if (user != null) {
                    // 웹사이트 접속 중이므로 isActive를 true로 설정
                    // 단, 이미 활성 타이머가 있으면 그대로 유지 (이미 true일 수 있음)
                    boolean wasActive = user.getIsActive();
                    log.info("WebSocket 연결: userId={}, wasActive={}, willSetActive=true", userId, wasActive);
                    user.setActive(true);
                    userRepository.save(user);
                    log.info("WebSocket 연결: 사용자 {}의 isActive를 true로 설정 완료", userId);
                    
                    // 항상 브로드캐스트 (상태가 변경되지 않아도 사용자가 들어왔다는 것을 알려야 함)
                    log.info("WebSocket 연결: 브로드캐스트 예약: userId={}, isActive=true (wasActive={})", userId, wasActive);
                    TransactionSynchronizationManager.registerSynchronization(
                        new TransactionSynchronization() {
                            @Override
                            public void afterCommit() {
                                log.info("WebSocket 연결: 트랜잭션 커밋 완료, 브로드캐스트 실행: userId={}, isActive=true", userId);
                                userActiveStatusService.broadcastActiveStatusChange(userId, true);
                            }
                        }
                    );
                } else {
                    log.warn("WebSocket 연결: 사용자를 찾을 수 없습니다. userId={}", userId);
                }
            } else {
                log.warn("WebSocket 연결: userId를 추출할 수 없습니다. sessionId={}", sessionId);
            }
        } catch (Exception e) {
            log.error("WebSocket 연결 시 isActive 업데이트 실패: {}", e.getMessage(), e);
        }
    }

    @EventListener
    @Transactional
    public void disconnectHandle(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        sessions.remove(accessor.getSessionId());
        log.info("===== WebSocket 해제 이벤트 ===== sessionId={}, total sessions={}", accessor.getSessionId(), sessions.size());

        // 세션 ID로 userId 찾기 (매핑에서 먼저 확인)
        try {
            String sessionId = accessor.getSessionId();
            UUID userId = sessionUserIdMap.get(sessionId);
            
            // 매핑에서 찾지 못하면 토큰에서 추출 시도
            if (userId == null) {
                log.warn("WebSocket 해제: 세션 매핑에서 userId를 찾을 수 없음, 토큰에서 추출 시도: sessionId={}", sessionId);
                userId = extractUserIdFromEvent(accessor);
            } else {
                log.info("WebSocket 해제: 세션 매핑에서 userId 찾음: sessionId={}, userId={}", sessionId, userId);
            }
            
            log.info("WebSocket 해제: sessionId={}, userId={}", sessionId, userId);
            if (userId != null) {
                // 세션 매핑에서 제거
                sessionUserIdMap.remove(sessionId);
                log.info("WebSocket 해제: 세션 매핑 제거: sessionId={}, userId={}", sessionId, userId);
                boolean removedFromLounge = officialLoungePresenceService.removePresenceIfNoWebSocketSession(userId);
                if (removedFromLounge) {
                    log.info("공식 라운지 presence 자동 제거 완료: userId={}", userId);
                    officialLoungeService.publishPresenceUpdate(null, userId, "DISCONNECT");
                }
                
                User user = userRepository.findById(userId).orElse(null);
                if (user != null) {
                    // 활성 타이머가 있는지 확인
                    boolean hasActiveTimer = activeFocusSessionRepository.findByUserId(userId).isPresent();
                    boolean wasActive = user.getIsActive();
                    // final 변수로 선언하여 inner class에서 참조 가능하도록 함
                    final boolean newActiveStatus;
                    
                    log.info("WebSocket 해제: userId={}, wasActive={}, hasActiveTimer={}", userId, wasActive, hasActiveTimer);
                    
                    // 활성 타이머가 없으면 isActive를 false로 설정
                    // 활성 타이머가 있으면 true로 유지 (타이머가 실행 중이므로)
                    if (!hasActiveTimer) {
                        user.setActive(false);
                        newActiveStatus = false;
                        userRepository.save(user);
                        log.info("WebSocket 해제: 사용자 {}의 isActive를 false로 설정 (활성 타이머 없음)", userId);
                    } else {
                        newActiveStatus = true;
                        log.info("WebSocket 해제: 사용자 {}의 isActive를 true로 유지 (활성 타이머 있음)", userId);
                    }
                    
                    // 항상 브로드캐스트 (상태가 변경되지 않아도 사용자가 나갔다는 것을 알려야 함)
                    final UUID finalUserId = userId; // final 변수로 선언
                    log.info("WebSocket 해제: 브로드캐스트 예약: userId={}, isActive={} (wasActive={})", finalUserId, newActiveStatus, wasActive);
                    TransactionSynchronizationManager.registerSynchronization(
                        new TransactionSynchronization() {
                            @Override
                            public void afterCommit() {
                                log.info("WebSocket 해제: 트랜잭션 커밋 완료, 브로드캐스트 실행: userId={}, isActive={}", finalUserId, newActiveStatus);
                                userActiveStatusService.broadcastActiveStatusChange(finalUserId, newActiveStatus);
                            }
                        }
                    );
                } else {
                    log.warn("WebSocket 해제: 사용자를 찾을 수 없습니다. userId={}", userId);
                }
            } else {
                log.warn("WebSocket 해제: userId를 추출할 수 없습니다. sessionId={}", sessionId);
            }
        } catch (Exception e) {
            log.error("WebSocket 해제 시 isActive 업데이트 실패: {}", e.getMessage(), e);
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
                log.warn("토큰을 찾을 수 없습니다. sessionId={}", accessor.getSessionId());
                log.warn("헤더에서 토큰 찾기 시도: bearerToken={}", bearerToken);
                log.warn("세션 속성: {}", accessor.getSessionAttributes());
                return null;
            }

            log.info("토큰 추출 성공, 토큰 길이: {}", token.length());

            // JwtUtil을 사용하여 userId 추출
            String userIdStr = jwtUtil.getUserIdFromToken(token);
            if (userIdStr == null || userIdStr.isEmpty()) {
                log.warn("토큰에서 userId를 추출할 수 없습니다. sessionId={}", accessor.getSessionId());
                return null;
            }

            log.info("userId 추출 성공: {}", userIdStr);
            return UUID.fromString(userIdStr);
        } catch (Exception e) {
            log.error("토큰에서 사용자 ID 추출 실패: {}", e.getMessage(), e);
            return null;
        }
    }
}
