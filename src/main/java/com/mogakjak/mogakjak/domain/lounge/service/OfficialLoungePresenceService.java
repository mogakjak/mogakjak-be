package com.mogakjak.mogakjak.domain.lounge.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OfficialLoungePresenceService {

    private static final String PRESENCE_KEY = "official-lounge:presence";
    private static final String SESSION_COUNT_KEY = "official-lounge:websocket-sessions";

    private final StringRedisTemplate stringRedisTemplate;
    private DefaultRedisScript<Long> enterScript;
    private DefaultRedisScript<Long> incrementSessionScript;
    private DefaultRedisScript<Long> decrementSessionScript;

    @PostConstruct
    public void init() {
        enterScript = new DefaultRedisScript<>();
        enterScript.setResultType(Long.class);
        enterScript.setScriptText("""
                local key = KEYS[1]
                local member = ARGV[1]
                local maxCount = tonumber(ARGV[2])
                local score = tonumber(ARGV[3])

                if redis.call('ZSCORE', key, member) then
                    redis.call('ZADD', key, score, member)
                    return 1
                end

                if redis.call('ZCARD', key) < maxCount then
                    redis.call('ZADD', key, score, member)
                    return 1
                end

                return 0
                """);

        incrementSessionScript = new DefaultRedisScript<>();
        incrementSessionScript.setResultType(Long.class);
        incrementSessionScript.setScriptText("""
                local key = KEYS[1]
                local member = ARGV[1]
                return redis.call('HINCRBY', key, member, 1)
                """);

        decrementSessionScript = new DefaultRedisScript<>();
        decrementSessionScript.setResultType(Long.class);
        decrementSessionScript.setScriptText("""
                local key = KEYS[1]
                local member = ARGV[1]
                local current = redis.call('HGET', key, member)

                if not current then
                    return 0
                end

                current = tonumber(current)
                if current <= 1 then
                    redis.call('HDEL', key, member)
                    return 0
                end

                return redis.call('HINCRBY', key, member, -1)
                """);
    }

    public boolean enter(UUID userId, int maxMemberCount) {
        Long result = stringRedisTemplate.execute(
                enterScript,
                List.of(PRESENCE_KEY),
                userId.toString(),
                String.valueOf(maxMemberCount),
                String.valueOf(Instant.now().toEpochMilli())
        );
        return result != null && result == 1L;
    }

    public boolean leave(UUID userId) {
        Long removed = stringRedisTemplate.opsForZSet().remove(PRESENCE_KEY, userId.toString());
        return removed != null && removed > 0L;
    }

    public boolean contains(UUID userId) {
        Double score = stringRedisTemplate.opsForZSet().score(PRESENCE_KEY, userId.toString());
        return score != null;
    }

    public long count() {
        Long count = stringRedisTemplate.opsForZSet().zCard(PRESENCE_KEY);
        return count != null ? count : 0L;
    }

    public List<UUID> findAllUserIds() {
        var rawIds = stringRedisTemplate.opsForZSet().range(PRESENCE_KEY, 0, -1);
        if (rawIds == null || rawIds.isEmpty()) {
            return List.of();
        }

        return rawIds.stream()
                .map(UUID::fromString)
                .toList();
    }

    public void remove(UUID userId) {
        stringRedisTemplate.opsForZSet().remove(PRESENCE_KEY, userId.toString());
    }

    public long registerWebSocketSession(UUID userId) {
        Long result = stringRedisTemplate.execute(
                incrementSessionScript,
                List.of(SESSION_COUNT_KEY),
                userId.toString()
        );
        return result != null ? result : 0L;
    }

    public long unregisterWebSocketSession(UUID userId) {
        Long result = stringRedisTemplate.execute(
                decrementSessionScript,
                List.of(SESSION_COUNT_KEY),
                userId.toString()
        );
        return result != null ? result : 0L;
    }

    public boolean removePresenceIfNoWebSocketSession(UUID userId) {
        long remainingSessions = unregisterWebSocketSession(userId);
        if (remainingSessions > 0) {
            return false;
        }

        return stringRedisTemplate.opsForZSet().remove(PRESENCE_KEY, userId.toString()) != null;
    }
}
