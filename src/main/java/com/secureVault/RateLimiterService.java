package com.secureVault;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class RateLimiterService {

    private final StringRedisTemplate redisTemplate;

    private static final String LUA_SCRIPT =
                    "local key = KEYS[1] " +
                    "local limit = tonumber(ARGV[1]) " +
                    "local current = tonumber(redis.call('get', key) or '0') " +
                    "if current + 1 > limit then " +
                    "  return 0 " +
                    "else " +
                    "  redis.call('INCRBY', key, 1) " +
                    "  if current==0 then " +
                    "    redis.call('EXPIRE', key,60) " +
                    "  end " +
                    "  return 1 " +
                    "end";

    boolean isAllowed(String clientIp, String bucket, int maxRequestsPerMinute) {
        String key = "rate:limit:" + bucket + ":" + clientIp;
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(LUA_SCRIPT, Long.class);
        Long result = redisTemplate.execute(script,
                Collections.singletonList(key),
                String.valueOf(maxRequestsPerMinute));

        return result == 1;

    }


}
