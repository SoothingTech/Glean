package com.soothingtech.glean.account.facade.impl;

import com.soothingtech.glean.account.facade.ConsentFacade;
import com.soothingtech.glean.account.facade.TokenFacade;
import com.soothingtech.glean.account.token.AuthPrincipal;
import com.soothingtech.glean.account.token.JwtService;
import com.soothingtech.glean.account.token.TokenBlacklist;
import com.soothingtech.glean.common.BizException;
import com.soothingtech.glean.common.ErrorCodes;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class TokenFacadeImpl implements TokenFacade {

    private static final String BEARER = "Bearer ";

    private final JwtService jwt;
    private final TokenBlacklist blacklist;
    private final ConsentFacade consentFacade;

    public TokenFacadeImpl(JwtService jwt, TokenBlacklist blacklist, ConsentFacade consentFacade) {
        this.jwt = jwt;
        this.blacklist = blacklist;
        this.consentFacade = consentFacade;
    }

    @Override
    public AuthPrincipal verify(String bearer) {
        String token = normalize(bearer);
        try {
            Claims claims = jwt.parse(token);
            if (!JwtService.TYPE_ACCESS.equals(claims.get(JwtService.CLAIM_TYPE, String.class))) {
                throw unauthorized();
            }
            UUID userId = UUID.fromString(claims.getSubject());
            String deviceId = claims.get(JwtService.CLAIM_DEVICE, String.class);
            Date issuedAt = claims.getIssuedAt();
            Instant issuedAtInstant = issuedAt == null ? null : issuedAt.toInstant();

            if (blacklist.isRevoked(token) || blacklist.isDeviceRevoked(userId, deviceId, issuedAtInstant)) {
                throw unauthorized();
            }
            return new AuthPrincipal(userId, deviceId, claims.get(JwtService.CLAIM_LEVEL, Integer.class));
        } catch (JwtException | IllegalArgumentException e) {
            throw unauthorized();
        }
    }

    @Override
    public String issue(UUID userId, String deviceId) {
        int level = consentFacade.currentLevel(userId).ordinal();
        return jwt.issueAccessToken(userId, deviceId, level);
    }

    private String normalize(String bearer) {
        if (bearer == null || bearer.isBlank()) {
            throw unauthorized();
        }
        return bearer.startsWith(BEARER) ? bearer.substring(BEARER.length()).trim() : bearer.trim();
    }

    private BizException unauthorized() {
        return new BizException(ErrorCodes.UNAUTHENTICATED, 401, "未认证或令牌已失效");
    }
}
