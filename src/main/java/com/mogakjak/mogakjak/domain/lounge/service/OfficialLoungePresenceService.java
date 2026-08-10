package com.mogakjak.mogakjak.domain.lounge.service;

import jakarta.annotation.PostConstruct;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OfficialLoungePresenceService {

    private static final String PRESENCE_KEY = "official-lounge:presence";
    private static final String SESSION_COUNT_KEY = "official-lounge:websocket-sessions";
    private static final String CHEER_COUNT_KEY = "official-lounge:cheer-count";

    private final StringRedisTemplate stringRedisTemplate;
    private final MeterRegistry meterRegistry;
    private DefaultRedisScript<Long> enterScript;
    private DefaultRedisScript<Long> incrementSessionScript;
    private DefaultRedisScript<Long> decrementSessionScript;

    @PostConstruct
    public void init() {
        Gauge.builder("mogakjak.lounge.members", this, OfficialLoungePresenceService::count)
                .description("현재 공식 라운지 입실 인원")
                .register(meterRegistry);

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
        removeCheerCount(userId);
        Long removed = stringRedisTemplate.opsForZSet().remove(PRESENCE_KEY, userId.toString());
        if (count() == 0L) {
            clearAllCheerCounts();
        }
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
        removeCheerCount(userId);
        stringRedisTemplate.opsForZSet().remove(PRESENCE_KEY, userId.toString());
        if (count() == 0L) {
            clearAllCheerCounts();
        }
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

        removeCheerCount(userId);
        boolean removed = stringRedisTemplate.opsForZSet().remove(PRESENCE_KEY, userId.toString()) != null;
        if (count() == 0L) {
            clearAllCheerCounts();
        }
        return removed;
    }

    public LocalDateTime getEnteredAt(UUID userId) {
        Double score = stringRedisTemplate.opsForZSet().score(PRESENCE_KEY, userId.toString());
        if (score == null) {
            return null;
        }

        return LocalDateTime.ofInstant(Instant.ofEpochMilli(score.longValue()), ZoneId.systemDefault());
    }

    public Map<UUID, LocalDateTime> getEnteredAtMap(List<UUID> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }

        List<Object> scores = stringRedisTemplate.executePipelined(new SessionCallback<>() {
            @Override
            @SuppressWarnings({"rawtypes", "unchecked"})
            public <K, V> Object execute(RedisOperations<K, V> operations) {
                RedisOperations rawOperations = operations;
                for (UUID userId : userIds) {
                    rawOperations.opsForZSet().score(PRESENCE_KEY, userId.toString());
                }
                return null;
            }
        });

        Map<UUID, LocalDateTime> enteredAtByUserId = new HashMap<>();
        for (int i = 0; i < userIds.size() && i < scores.size(); i++) {
            Object rawScore = scores.get(i);
            if (rawScore == null) {
                continue;
            }

            double score = rawScore instanceof Number number
                    ? number.doubleValue()
                    : Double.parseDouble(rawScore.toString());
            enteredAtByUserId.put(
                    userIds.get(i),
                    LocalDateTime.ofInstant(Instant.ofEpochMilli((long) score), ZoneId.systemDefault())
            );
        }

        return enteredAtByUserId;
    }

    public Integer getCheerCount(UUID userId) {
        Object value = stringRedisTemplate.opsForHash().get(CHEER_COUNT_KEY, userId.toString());
        if (value == null) {
            return 0;
        }

        if (value instanceof Number number) {
            return number.intValue();
        }

        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public Map<UUID, Integer> getCheerCountMap(List<UUID> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }

        List<Object> counts = stringRedisTemplate.executePipelined(new SessionCallback<>() {
            @Override
            @SuppressWarnings({"rawtypes", "unchecked"})
            public <K, V> Object execute(RedisOperations<K, V> operations) {
                RedisOperations rawOperations = operations;
                for (UUID userId : userIds) {
                    rawOperations.opsForHash().get(CHEER_COUNT_KEY, userId.toString());
                }
                return null;
            }
        });

        Map<UUID, Integer> cheerCountByUserId = new HashMap<>();
        for (int i = 0; i < userIds.size() && i < counts.size(); i++) {
            Object rawCount = counts.get(i);
            if (rawCount == null) {
                continue;
            }

            cheerCountByUserId.put(userIds.get(i), parseCheerCount(rawCount));
        }

        return cheerCountByUserId;
    }

    public Long incrementCheerCount(UUID userId) {
        Long result = stringRedisTemplate.opsForHash().increment(CHEER_COUNT_KEY, userId.toString(), 1L);
        return result != null ? result : 0L;
    }

    public void removeCheerCount(UUID userId) {
        stringRedisTemplate.opsForHash().delete(CHEER_COUNT_KEY, userId.toString());
    }

    public void clearAllCheerCounts() {
        stringRedisTemplate.delete(CHEER_COUNT_KEY);
    }

    private Integer parseCheerCount(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }

        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
