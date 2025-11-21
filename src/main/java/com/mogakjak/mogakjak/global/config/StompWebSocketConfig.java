package com.mogakjak.mogakjak.global.config;

import com.mogakjak.mogakjak.global.websocket.util.StompHandler;
import com.mogakjak.mogakjak.global.websocket.util.WebSocketHandshakeInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableWebSocketMessageBroker
public class StompWebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private final StompHandler stompHandler;
    private final WebSocketHandshakeInterceptor handshakeInterceptor;

    @Value("${frontend.base-url:https://mogakjak.site}")
    private String frontendBaseUrl;

    @Value("#{'${app.oauth2.authorized-redirect-uris}'.split(',')}")
    private List<String> authorizedRedirectUris;

    public StompWebSocketConfig(StompHandler stompHandler, WebSocketHandshakeInterceptor handshakeInterceptor) {
        this.stompHandler = stompHandler;
        this.handshakeInterceptor = handshakeInterceptor;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 허용할 Origin 목록 구성
        List<String> allowedOrigins = new ArrayList<>();
        allowedOrigins.add("http://localhost:3000");
        allowedOrigins.add("http://localhost:3002");
        allowedOrigins.add("https://mogakjak.site");
        allowedOrigins.add("http://mogakjak.site");

        // authorized-redirect-uris에서도 추가
        for (String uri : authorizedRedirectUris) {
            String trimmed = uri.trim();
            if (!trimmed.isEmpty()) {
                // http:// 또는 https:// 제거하고 도메인만 추출
                String domain = trimmed.replaceAll("^https?://", "");
                if (!domain.isEmpty()) {
                    String httpsUrl = "https://" + domain;
                    String httpUrl = "http://" + domain;
                    if (!allowedOrigins.contains(httpsUrl)) {
                        allowedOrigins.add(httpsUrl);
                    }
                    if (!allowedOrigins.contains(httpUrl)) {
                        allowedOrigins.add(httpUrl);
                    }
                }
            }
        }

        registry.addEndpoint("/connect")
                .setAllowedOrigins(allowedOrigins.toArray(new String[0]))
//                ws://가 아닌 http:// 엔드포인트를 사용할수 있게 해주는 sockJs라이브러리를 통한 요청을 허용하는 설정.
                .addInterceptors(handshakeInterceptor)
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
//        /publish/1형태로 메시지 발행해야 함을 설정
//        /publish로 시작하는 url패턴으로 메시지가 발행되면 @Controller 객체의 @MessaMapping메서드로 라우팅
        registry.setApplicationDestinationPrefixes("/publish");

//        /topic/1형태로 메시지를 수신(subscribe)해야 함을 설정
        registry.enableSimpleBroker("/topic");

    }


//    웹소켓요청(connect, subscribe, disconnect)등의 요청시에는 http header등 http메시지를 넣어올수 있고, 이를 interceptor를 통해 가로채 토큰등을 검증할수 있음.
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompHandler);
    }

}
