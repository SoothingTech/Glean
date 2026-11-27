package com.soothingtech.glean.account.app;

import com.soothingtech.glean.account.api.dto.TokenPair;
import com.soothingtech.glean.account.domain.AppUser;
import com.soothingtech.glean.account.domain.AppUserRepository;
import com.soothingtech.glean.account.token.JwtService;
import com.soothingtech.glean.account.token.TokenBlacklist;
import com.soothingtech.glean.common.BizException;
import com.soothingtech.glean.common.ErrorCodes;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class AuthService {

    /** 新用户默认授权级别 */
    private static final short DEFAULT_LEVEL = 10;
    private static final String UNKNOWN_DEVICE = "unknown";

    private final AppUserRepository users;
    private final SmsCodeService smsCodes;
    private final JwtService jwt;
    private final TokenBlacklist blacklist;
    private final DeviceService devices;
    private final AuditService audit;

    public AuthService(AppUserRepository users,
                       SmsCodeService smsCodes,
                       JwtService jwt,
                       TokenBlacklist blacklist,
                       DeviceService devices,
                       AuditService audit) {
        this.users = users;
        this.smsCodes = smsCodes;
        this.jwt = jwt;
        this.blacklist = blacklist;
        this.devices = devices;
        this.audit = audit;
    }

    public Duration sendCode(String phone) {
        return smsCodes.sendCode(phone);
    }

    /** 登录：校验验证码 → 找不到账号则自动注册 → 登记设备 → 签发双令牌 → 写审计 */
    @Transactional
    public TokenPair login(String phone, String code, String deviceId, String platform) {
        smsCodes.verify(phone, code);

        AppUser user = users.findByPhone(phone)
                .orElseGet(() -> users.save(new AppUser(UUID.randomUUID(), phone, DEFAULT_LEVEL)));

        String device = devices.touch(user.getId(), normalizeDevice(deviceId), platform);
        TokenPair tokens = issueTokens(user, device);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("deviceId", device);
        payload.put("platform", platform);
        audit.record(user.getId(), AuditService.ACTION_LOGIN, payload);

        return tokens;
    }

    /** 刷新：只接受 typ=refresh、未被登出、且晚于设备撤销水位的令牌 */
    public TokenPair refresh(String refreshToken, String deviceId) {
        Claims claims;
        try {
            claims = jwt.parse(refreshToken);
        } catch (JwtException | IllegalArgumentException e) {
            throw invalidRefresh();
        }

        UUID userId;
        try {
            userId = UUID.fromString(claims.getSubject());
        } catch (IllegalArgumentException e) {
            throw invalidRefresh();
        }
        String tokenDevice = claims.get(JwtService.CLAIM_DEVICE, String.class);

        if (!JwtService.TYPE_REFRESH.equals(claims.get(JwtService.CLAIM_TYPE, String.class))
                || blacklist.isRevoked(refreshToken)
                || blacklist.isDeviceRevoked(userId, tokenDevice, issuedAt(claims))) {
            throw invalidRefresh();
        }

        AppUser user = users.findById(userId).orElseThrow(this::invalidRefresh);
        String device = deviceId != null && !deviceId.isBlank()
                ? deviceId
                : normalizeDevice(tokenDevice);
        return issueTokens(user, device);
    }

    /**
     * 登出：撤销当前令牌，并把整台设备的会话水位推进到现在，
     * 使同一设备的 refresh token 也一并失效；全过程写审计。
     */
    public void logout(String token) {
        try {
            Claims claims = jwt.parse(token);
            Instant expiresAt = claims.getExpiration().toInstant();
            blacklist.revoke(token, expiresAt);

            UUID userId = UUID.fromString(claims.getSubject());
            String deviceId = claims.get(JwtService.CLAIM_DEVICE, String.class);
            blacklist.revokeDevice(userId, deviceId, jwt.getRefreshTtl());

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("deviceId", deviceId);
            audit.record(userId, AuditService.ACTION_LOGOUT, payload);
        } catch (JwtException | IllegalArgumentException ignored) {
            // 幂等：令牌本就无效，登出仍然算成功
        }
    }

    private TokenPair issueTokens(AppUser user, String deviceId) {
        String accessToken = jwt.issueAccessToken(user.getId(), deviceId, user.getLvl());
        String refreshToken = jwt.issueRefreshToken(user.getId(), deviceId, user.getLvl());
        return new TokenPair(accessToken, refreshToken, jwt.getAccessTtl().toSeconds());
    }

    private Instant issuedAt(Claims claims) {
        Date value = claims.getIssuedAt();
        return value == null ? null : value.toInstant();
    }

    private String normalizeDevice(String deviceId) {
        return deviceId == null || deviceId.isBlank() ? UNKNOWN_DEVICE : deviceId;
    }

    private BizException invalidRefresh() {
        return new BizException(ErrorCodes.UNAUTHENTICATED, 401, "刷新令牌无效或已过期");
    }
}
