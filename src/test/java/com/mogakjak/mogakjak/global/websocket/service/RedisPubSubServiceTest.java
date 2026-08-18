package com.mogakjak.mogakjak.global.websocket.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.core.StringRedisTemplate;

@ExtendWith(MockitoExtension.class)
class RedisPubSubServiceTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private RealtimeMessageDispatcher messageDispatcher;

    @Mock
    private RealtimeMetrics realtimeMetrics;

    @Mock
    private Message message;

    @Test
    void publish_recordsMetricsAndPublishesToRedis() {
        RedisPubSubService service = new RedisPubSubService(stringRedisTemplate, messageDispatcher, realtimeMetrics);
        doAnswer(invocation -> {
            invocation.<Runnable>getArgument(3).run();
            return null;
        }).when(realtimeMetrics).recordPublish(eq("redis"), eq("chat"), eq("payload"), any(Runnable.class));

        service.publish("chat", "payload");

        verify(stringRedisTemplate).convertAndSend("chat", "payload");
    }

    @Test
    void onMessage_delegatesPayloadToSharedDispatcher() {
        RedisPubSubService service = new RedisPubSubService(stringRedisTemplate, messageDispatcher, realtimeMetrics);
        when(message.getBody()).thenReturn("payload".getBytes(StandardCharsets.UTF_8));

        service.onMessage(message, "official-lounge-presence".getBytes(StandardCharsets.UTF_8));

        verify(messageDispatcher).dispatch("redis", "official-lounge-presence", "payload");
    }
}
