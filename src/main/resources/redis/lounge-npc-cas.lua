-- 현재 실제 인원과 NPC 상태를 함께 확인해 동시 입퇴장/스케줄 실행의 중복 적용을 막는다.
local current = redis.call('GET', KEYS[1]) or ''
if current ~= ARGV[1] then
    return 0
end
if redis.call('ZCARD', KEYS[2]) ~= tonumber(ARGV[2]) then
    return 0
end
redis.call('SET', KEYS[1], ARGV[3])
return 1
