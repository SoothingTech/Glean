package com.soothingtech.glean.account.app;

import com.soothingtech.glean.common.BizException;
import com.soothingtech.glean.common.ErrorCodes;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

/**
 * 写接口幂等。5.7.3 第 49 行：Redis 键 idem:{userId}:{key}，TTL 24h，
 * 同键不同载荷返回 E5004（409）。
 * 键来源：请求头 Idempotency-Key 或请求体 clientRequestId，二者取其一。
 */
@Service
public class IdempotencyService {

    private static final String PREFIX = "idem:";
    private static final Duration TTL = Duration.ofHours(24);

    private final StringRedisTemplate redis;

    public IdempotencyService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /**
     * 首次出现则登记载荷指纹；重复且指纹不同直接拒绝。
     * 指纹相同视为同一请求的重放，按幂等放行。
     */
    public void check(UUID userId, String key, String fingerprint) {
        if (key == null || key.isBlank()) {
            return;
        }
        String redisKey = PREFIX + userId + ":" + key;
        String existing = redis.opsForValue().get(redisKey);
        if (existing == null) {
            redis.opsForValue().set(redisKey, fingerprint, TTL);
            return;
        }
        if (!existing.equals(fingerprint)) {
            throw new BizException(ErrorCodes.IDEMPOTENT_CONFLICT, 409, "幂等键复用但载荷不同");
        }
    }
}
