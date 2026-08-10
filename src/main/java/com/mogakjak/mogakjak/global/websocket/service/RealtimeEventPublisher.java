package com.mogakjak.mogakjak.global.websocket.service;

public interface RealtimeEventPublisher {

    void publish(String channel, String payload);
}
