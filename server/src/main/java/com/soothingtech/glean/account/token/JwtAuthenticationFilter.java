package com.soothingtech.glean.account.token;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * 从 Authorization: Bearer 解析 JWT 并写入 SecurityContext。
 * 令牌无效 / 已被登出 / 早于设备撤销水位，都不抛异常，
 * 交给 AuthenticationEntryPoint 统一返回 E5001。
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER = "Bearer ";

    private final JwtService jwtService;
    private final TokenBlacklist blacklist;

    public JwtAuthenticationFilter(JwtService jwtService, TokenBlacklist blacklist) {
        this.jwtService = jwtService;
        this.blacklist = blacklist;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(BEARER)) {
            String token = header.substring(BEARER.length()).trim();
            if (!token.isEmpty() && !blacklist.isRevoked(token)) {
                authenticate(token);
            }
        }
        chain.doFilter(request, response);
    }

    private void authenticate(String token) {
        try {
            Claims claims = jwtService.parse(token);
            if (!JwtService.TYPE_ACCESS.equals(claims.get(JwtService.CLAIM_TYPE, String.class))) {
                return;
            }
            UUID userId = UUID.fromString(claims.getSubject());
            String deviceId = claims.get(JwtService.CLAIM_DEVICE, String.class);
            Date issuedAt = claims.getIssuedAt();
            Instant issuedAtInstant = issuedAt == null ? null : issuedAt.toInstant();

            if (blacklist.isDeviceRevoked(userId, deviceId, issuedAtInstant)) {
                return;
            }

            AuthPrincipal principal = new AuthPrincipal(
                    userId,
                    deviceId,
                    claims.get(JwtService.CLAIM_LEVEL, Integer.class));
            var authentication = new UsernamePasswordAuthenticationToken(
                    principal, null, AuthorityUtils.createAuthorityList("ROLE_USER"));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (JwtException | IllegalArgumentException ignored) {
            // 无效令牌：不设置认证
        }
    }
}
