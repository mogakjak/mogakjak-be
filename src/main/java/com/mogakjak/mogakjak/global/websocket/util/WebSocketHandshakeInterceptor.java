package com.mogakjak.mogakjak.global.websocket.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * 웹소켓 핸드셰이크 시 쿠키에서 토큰을 읽어서 STOMP 헤더에 추가하는 인터셉터
 */
@Slf4j
@Component
public class WebSocketHandshakeInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        if (request instanceof ServletServerHttpRequest) {
            ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
            HttpServletRequest httpRequest = servletRequest.getServletRequest();
            
            String origin = httpRequest.getHeader("Origin");
            String requestURI = httpRequest.getRequestURI();
            log.info("WebSocket 핸드셰이크 시작: Origin={}, RequestURI={}", origin, requestURI);
            
            // 쿠키에서 토큰 찾기
            Cookie[] cookies = httpRequest.getCookies();
            if (cookies == null) {
                log.warn("WebSocket 핸드셰이크: 쿠키가 없습니다. Origin={}", origin);
            } else {
                log.info("WebSocket 핸드셰이크: 쿠키 개수={}", cookies.length);
                boolean tokenFound = false;
                for (Cookie cookie : cookies) {
                    log.info("쿠키 이름: {}, 값 길이: {}", cookie.getName(), 
                            cookie.getValue() != null ? cookie.getValue().length() : 0);
                    if ("mg_access_token".equals(cookie.getName())) {
                        String token = cookie.getValue();
                        if (token != null && !token.isEmpty()) {
                            // STOMP 헤더에 토큰 추가
                            attributes.put("token", token);
                            log.info("WebSocket 핸드셰이크: 토큰을 세션 속성에 저장했습니다. 토큰 길이={}", token.length());
                            tokenFound = true;
                            break;
                        }
                    }
                }
                if (!tokenFound) {
                    log.warn("WebSocket 핸드셰이크: mg_access_token 쿠키를 찾을 수 없습니다. Origin={}", origin);
                    log.warn("WebSocket 핸드셰이크: 도메인이 다르면 쿠키가 전달되지 않을 수 있습니다.");
                }
            }
        }
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                              WebSocketHandler wsHandler, Exception exception) {
        // 핸드셰이크 완료 후 처리할 로직이 있으면 여기에 작성
    }
}

