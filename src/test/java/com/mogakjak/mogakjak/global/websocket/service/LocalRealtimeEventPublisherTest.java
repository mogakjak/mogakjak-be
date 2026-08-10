package com.mogakjak.mogakjak.global.websocket.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LocalRealtimeEventPublisherTest {

    @Mock
    private RealtimeMessageDispatcher messageDispatcher;

    @Mock
    private RealtimeMetrics realtimeMetrics;

    @Test
    void publish_recordsMetricsAndDispatchesWithoutRedis() {
        LocalRealtimeEventPublisher publisher =
                new LocalRealtimeEventPublisher(messageDispatcher, realtimeMetrics);
        doAnswer(invocation -> {
            invocation.<Runnable>getArgument(3).run();
            return null;
        }).when(realtimeMetrics).recordPublish(eq("local"), eq("chat"), eq("payload"), any(Runnable.class));

        publisher.publish("chat", "payload");

        verify(messageDispatcher).dispatch("local", "chat", "payload");
    }
}
