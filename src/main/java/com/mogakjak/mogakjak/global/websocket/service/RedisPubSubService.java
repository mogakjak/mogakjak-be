package com.mogakjak.mogakjak.global.websocket.service;

import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "app.realtime", name = "transport", havingValue = "redis", matchIfMissing = true)
public class RedisPubSubService implements MessageListener, RealtimeEventPublisher {

    private static final String TRANSPORT = "redis";

    private final StringRedisTemplate stringRedisTemplate;
    private final RealtimeMessageDispatcher messageDispatcher;
    private final RealtimeMetrics realtimeMetrics;

    public RedisPubSubService(
            @Qualifier("chatPubSub") StringRedisTemplate stringRedisTemplate,
            RealtimeMessageDispatcher messageDispatcher,
            RealtimeMetrics realtimeMetrics
    ) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.messageDispatcher = messageDispatcher;
        this.realtimeMetrics = realtimeMetrics;
    }

    @Override
    public void publish(String channel, String payload) {
        realtimeMetrics.recordPublish(
                TRANSPORT,
                channel,
                payload,
                () -> stringRedisTemplate.convertAndSend(channel, payload)
        );
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String payload = new String(message.getBody(), StandardCharsets.UTF_8);
        String channel = new String(pattern, StandardCharsets.UTF_8);
        messageDispatcher.dispatch(TRANSPORT, channel, payload);
    }
}
