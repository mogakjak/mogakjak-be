package com.mogakjak.mogakjak.domain.lounge.npc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mogakjak.mogakjak.domain.lounge.dto.LoungeNpcMemberResponse;
import com.mogakjak.mogakjak.domain.user.entity.GroupParticipationStatus;
import com.mogakjak.mogakjak.global.websocket.dto.OfficialLoungePresenceUpdateDto;
import com.mogakjak.mogakjak.global.websocket.service.RedisPubSubService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.config.annotation.*;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

/** 실제 로컬 Redis → 기존 PubSub 라우터 → STOMP 구독자까지 NPC 계약을 검증한다. */
@EnabledIfEnvironmentVariable(named = "NPC_REDIS_TEST_PORT", matches = "[0-9]+")
class LoungeNpcWebSocketRedisTest {
    @Test
    void receivesNpcPayloadThroughActualRedisAndWebSocket() throws Exception {
        try (var context = new AnnotationConfigServletWebServerApplicationContext(WebConfig.class)) {
            var factory = new LettuceConnectionFactory("127.0.0.1", Integer.parseInt(System.getenv("NPC_REDIS_TEST_PORT")));
            factory.afterPropertiesSet();
            factory.start();
            var redis = new StringRedisTemplate(factory);
            var listener = new RedisMessageListenerContainer();
            listener.setConnectionFactory(factory);
            listener.addMessageListener(new RedisPubSubService(redis, context.getBean(SimpMessagingTemplate.class)),
                    new PatternTopic("official-lounge-presence"));
            var client = new WebSocketStompClient(new StandardWebSocketClient());
            client.setMessageConverter(new MappingJackson2MessageConverter());
            StompSession session = null;
            try {
                listener.afterPropertiesSet();
                listener.start();
                int port = context.getWebServer().getPort();
                session = client.connectAsync("ws://127.0.0.1:" + port + "/npc-test-ws",
                        new StompSessionHandlerAdapter() {}).get(60, TimeUnit.SECONDS);
                BlockingQueue<Object> received = new LinkedBlockingQueue<>();
                session.subscribe("/topic/lounge/presence", new StompFrameHandler() {
                    @Override public Type getPayloadType(StompHeaders headers) { return Map.class; }
                    @Override public void handleFrame(StompHeaders headers, Object payload) { received.add(payload); }
                });
                var npc = new LoungeNpcMemberResponse("A", LoungeNpcType.A, "모각이", null, 1,
                        GroupParticipationStatus.PARTICIPATING, LocalDateTime.of(2026, 11, 4, 18, 0), 30L,
                        "코딩테스트 준비");
                var payload = OfficialLoungePresenceUpdateDto.builder().eventType("NPC_UPDATE")
                        .currentMemberCount(0L).displayMemberCount(1L).maxMemberCount(10)
                        .members(List.of()).npcMembers(List.of(npc)).build();
                var mapper = new ObjectMapper().registerModule(new JavaTimeModule());
                String json = mapper.writeValueAsString(payload);
                Object delivered = null;
                // SUBSCRIBE 처리 완료 전 발행과 경쟁할 수 있으므로 테스트 메시지만 재발행한다.
                for (int attempt = 0; attempt < 25 && delivered == null; attempt++) {
                    redis.convertAndSend("official-lounge-presence", json);
                    delivered = received.poll(200, TimeUnit.MILLISECONDS);
                }
                assertNotNull(delivered, "NPC presence 메시지를 실제 STOMP 구독자가 받아야 한다.");
                var actual = mapper.valueToTree(delivered);
                assertEquals("NPC_UPDATE", actual.get("eventType").textValue());
                assertEquals(0, actual.get("currentMemberCount").intValue());
                assertEquals(1, actual.get("displayMemberCount").intValue());
                assertEquals("A", actual.get("npcMembers").get(0).get("npcId").textValue());
                assertEquals("2026-11-04T18:00:00", actual.get("npcMembers").get(0).get("enteredAt").textValue());
                assertEquals(30, actual.get("npcMembers").get(0).get("personalTimerSeconds").intValue());
                assertEquals(0, actual.get("members").size());
            } finally {
                if (session != null && session.isConnected()) session.disconnect();
                client.stop();
                listener.destroy();
                factory.destroy();
            }
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableWebMvc
    @EnableWebSocketMessageBroker
    static class WebConfig implements WebSocketMessageBrokerConfigurer {
        @Bean
        TomcatServletWebServerFactory webServer() throws Exception {
            var factory = new TomcatServletWebServerFactory(0);
            factory.setAddress(InetAddress.getByName("127.0.0.1"));
            factory.addContextCustomizers(context -> context.addServletContainerInitializer(
                    new org.apache.tomcat.websocket.server.WsSci(), null));
            return factory;
        }

        @Bean
        ServletRegistrationBean<DispatcherServlet> dispatcher(WebApplicationContext context) {
            var registration = new ServletRegistrationBean<>(new DispatcherServlet(context), "/");
            registration.setLoadOnStartup(1);
            return registration;
        }

        @Override
        public void registerStompEndpoints(StompEndpointRegistry registry) {
            registry.addEndpoint("/npc-test-ws");
        }

        @Override
        public void configureMessageBroker(MessageBrokerRegistry registry) {
            registry.enableSimpleBroker("/topic");
        }
    }
}
