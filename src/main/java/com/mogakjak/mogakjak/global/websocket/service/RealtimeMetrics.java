package com.mogakjak.mogakjak.global.websocket.service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RealtimeMetrics {

    private final MeterRegistry meterRegistry;

    public void recordPublish(String transport, String channel, String payload, Runnable operation) {
        meterRegistry.summary(
                        "mogakjak.realtime.payload.bytes",
                        "transport", transport,
                        "channel", channel
                )
                .record(payload.getBytes(StandardCharsets.UTF_8).length);

        Timer.Sample sample = Timer.start(meterRegistry);
        String result = "success";
        try {
            operation.run();
        } catch (RuntimeException exception) {
            result = "failure";
            throw exception;
        } finally {
            meterRegistry.counter(
                            "mogakjak.realtime.messages",
                            "transport", transport,
                            "channel", channel,
                            "stage", "published",
                            "result", result
                    )
                    .increment();
            sample.stop(meterRegistry.timer(
                    "mogakjak.realtime.publish",
                    "transport", transport,
                    "channel", channel,
                    "result", result
            ));
        }
    }

    public void recordReceived(String transport, String channel) {
        meterRegistry.counter(
                        "mogakjak.realtime.messages",
                        "transport", transport,
                        "channel", channel,
                        "stage", "received",
                        "result", "success"
                )
                .increment();
    }

    public void recordDeliveries(String transport, String channel, int count) {
        if (count <= 0) {
            return;
        }

        meterRegistry.counter(
                        "mogakjak.realtime.deliveries",
                        "transport", transport,
                        "channel", channel
                )
                .increment(count);
    }
}
