package com.mogakjak.mogakjak.domain.lounge.npc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mogakjak.mogakjak.domain.lounge.service.OfficialLoungePresenceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class LoungeNpcStore {
    private final StringRedisTemplate redis;
    private final String stateKey;
    private final String presenceKey;
    private final ObjectMapper mapper = new ObjectMapper();
    private final DefaultRedisScript<Long> casScript = new DefaultRedisScript<>();

    @Autowired
    public LoungeNpcStore(StringRedisTemplate redis) {
        this(redis, "official-lounge:npc:state", OfficialLoungePresenceService.PRESENCE_KEY);
    }

    LoungeNpcStore(StringRedisTemplate redis, String stateKey, String presenceKey) {
        this.redis = redis;
        this.stateKey = stateKey;
        this.presenceKey = presenceKey;
        casScript.setLocation(new ClassPathResource("redis/lounge-npc-cas.lua"));
        casScript.setResultType(Long.class);
    }

    public Stored read() {
        String raw = redis.opsForValue().get(stateKey);
        try {
            return new Stored(raw, raw == null ? null : mapper.readValue(raw, LoungeNpcSnapshot.class));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("NPC 상태를 읽을 수 없습니다.", e);
        }
    }

    public int realMemberCount() {
        Long count = redis.opsForZSet().zCard(presenceKey);
        return count == null ? 0 : Math.toIntExact(count);
    }

    /** NPC 상태와 계산 기준 실제 인원이 그대로일 때에만 다음 상태를 저장한다. */
    public boolean compareAndSet(Stored expected, int realCount, LoungeNpcSnapshot next) {
        try {
            Long result = redis.execute(casScript, List.of(stateKey, presenceKey),
                    expected.raw() == null ? "" : expected.raw(), String.valueOf(realCount),
                    mapper.writeValueAsString(next));
            return Long.valueOf(1L).equals(result);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("NPC 상태를 저장할 수 없습니다.", e);
        }
    }

    public record Stored(String raw, LoungeNpcSnapshot snapshot) {}
}
