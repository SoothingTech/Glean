package com.soothingtech.glean.account.token;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

/**
 * 令牌撤销，两条并行机制：
 *
 * <p>1. 单令牌黑名单：文档 5.7.3.2 第 131 行要求登出后令牌入黑名单，TTL = 剩余有效期。
 * 以令牌 SHA-256 作键，避免明文令牌进 Redis，也不必给 claims 增加 jti。
 *
 * <p>2. 设备级撤销水位：只黑名单 access token 的话，同一设备的 refresh token 仍然有效，
 * 登出后能立刻换出新令牌，等于没登出。因此登出时再写一条 {userId, device} 的撤销时间戳，
 * 任何 iat 早于该时间戳的令牌（access 与 refresh 都算）一律拒绝。
 * 重新登录产生的新令牌 iat 更晚，不受影响。
 */
@Component
public class TokenBlacklist {

    private static final String TOKEN_PREFIX = "auth:blacklist:";
    private static final String DEVICE_PREFIX = "auth:revoked:";

    private final StringRedisTemplate redis;

    public TokenBlacklist(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /** 精确撤销单个令牌 */
    public void revoke(String token, Instant expiresAt) {
        long seconds = Duration.between(Instant.now(), expiresAt).toSeconds();
        if (seconds <= 0) {
            return;
        }
        redis.opsForValue().set(TOKEN_PREFIX + sha256(token), "1", Duration.ofSeconds(seconds));
    }

    public boolean isRevoked(String token) {
        return Boolean.TRUE.equals(redis.hasKey(TOKEN_PREFIX + sha256(token)));
    }

    /** 撤销某用户在某设备上的整个会话（覆盖其 refresh token） */
    public void revokeDevice(UUID userId, String deviceId, Duration ttl) {
        if (deviceId == null || deviceId.isBlank()) {
            return;
        }
        redis.opsForValue().set(
                DEVICE_PREFIX + userId + ":" + deviceId,
                String.valueOf(Instant.now().getEpochSecond()),
                ttl);
    }

    /** 该令牌的签发时间是否早于设备的撤销水位 */
    public boolean isDeviceRevoked(UUID userId, String deviceId, Instant issuedAt) {
        if (deviceId == null || deviceId.isBlank()) {
            return false;
        }
        String value = redis.opsForValue().get(DEVICE_PREFIX + userId + ":" + deviceId);
        if (value == null) {
            return false;
        }
        try {
            long revokedAt = Long.parseLong(value);
            return issuedAt == null || issuedAt.getEpochSecond() <= revokedAt;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private String sha256(String token) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("JVM 不支持 SHA-256", e);
        }
    }
}
