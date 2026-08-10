package com.mogakjak.mogakjak.global.websocket.service;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.realtime", name = "transport", havingValue = "local")
public class LocalRealtimeEventPublisher implements RealtimeEventPublisher {

    private static final String TRANSPORT = "local";

    private final RealtimeMessageDispatcher messageDispatcher;
    private final RealtimeMetrics realtimeMetrics;

    @Override
    public void publish(String channel, String payload) {
        realtimeMetrics.recordPublish(
                TRANSPORT,
                channel,
                payload,
                () -> messageDispatcher.dispatch(TRANSPORT, channel, payload)
        );
    }
}
