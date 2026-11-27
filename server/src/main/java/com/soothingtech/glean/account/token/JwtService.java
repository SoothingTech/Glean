package com.soothingtech.glean.account.token;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * JWT 签发与解析。
 * 文档 5.7.3.2 第 186 行：claims 固定为 sub / dev / lvl / iat / exp。
 * 额外增加 typ 用于区分 access / refresh —— 否则刷新令牌可当访问令牌用，
 * 属安全漏洞，这一处与文档的偏差已在《AI 辅助编程交互记录》中登记。
 */
@Service
public class JwtService {

    public static final String CLAIM_DEVICE = "dev";
    public static final String CLAIM_LEVEL = "lvl";
    public static final String CLAIM_TYPE = "typ";
    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";

    private final SecretKey key;
    private final Duration accessTtl;
    private final Duration refreshTtl;

    public JwtService(@Value("${glean.auth.jwt-secret}") String secret,
                      @Value("${glean.auth.access-ttl}") Duration accessTtl,
                      @Value("${glean.auth.refresh-ttl}") Duration refreshTtl) {
        byte[] raw = secret.getBytes(StandardCharsets.UTF_8);
        if (raw.length < 32) {
            throw new IllegalStateException("glean.auth.jwt-secret 至少需要 32 字节（HS256 要求）");
        }
        this.key = Keys.hmacShaKeyFor(raw);
        this.accessTtl = accessTtl;
        this.refreshTtl = refreshTtl;
    }

    public String issueAccessToken(UUID userId, String deviceId, int lvl) {
        return issue(userId, deviceId, lvl, TYPE_ACCESS, accessTtl);
    }

    public String issueRefreshToken(UUID userId, String deviceId, int lvl) {
        return issue(userId, deviceId, lvl, TYPE_REFRESH, refreshTtl);
    }

    private String issue(UUID userId, String deviceId, int lvl, String type, Duration ttl) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .claim(CLAIM_DEVICE, deviceId)
                .claim(CLAIM_LEVEL, lvl)
                .claim(CLAIM_TYPE, type)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .signWith(key)
                .compact();
    }

    /**
     * 解析并校验签名与有效期。
     *
     * @throws io.jsonwebtoken.JwtException 签名不合法或已过期
     */
    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Duration getAccessTtl() {
        return accessTtl;
    }

    public Duration getRefreshTtl() {
        return refreshTtl;
    }
}
