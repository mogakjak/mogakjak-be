package com.mogakjak.mogakjak.global.websocket.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

class RealtimeMetricsTest {

    @Test
    void recordPublish_recordsPayloadCountAndDuration() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        RealtimeMetrics metrics = new RealtimeMetrics(meterRegistry);

        metrics.recordPublish("local", "chat", "안녕", () -> { });

        assertEquals(1.0, meterRegistry.get("mogakjak.realtime.messages")
                .tags("transport", "local", "channel", "chat", "stage", "published", "result", "success")
                .counter()
                .count());
        assertEquals(6.0, meterRegistry.get("mogakjak.realtime.payload.bytes")
                .tags("transport", "local", "channel", "chat")
                .summary()
                .totalAmount());
        assertEquals(1L, meterRegistry.get("mogakjak.realtime.publish")
                .tags("transport", "local", "channel", "chat", "result", "success")
                .timer()
                .count());
    }
}
