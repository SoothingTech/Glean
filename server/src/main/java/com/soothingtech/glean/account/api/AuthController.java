package com.soothingtech.glean.account.api;

import com.soothingtech.glean.account.api.dto.LoginRequest;
import com.soothingtech.glean.account.api.dto.MeResponse;
import com.soothingtech.glean.account.api.dto.RefreshRequest;
import com.soothingtech.glean.account.api.dto.SmsCodeRequest;
import com.soothingtech.glean.account.api.dto.TokenPair;
import com.soothingtech.glean.account.app.AuthService;
import com.soothingtech.glean.account.token.AuthPrincipal;
import com.soothingtech.glean.common.ApiResponse;
import com.soothingtech.glean.common.BizException;
import com.soothingtech.glean.common.ErrorCodes;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Map;

/**
 * 认证接口（5.7.3.2 第 129—131 行）。
 * /sms-code 为新增：文档只列了 login/refresh/logout，但没有下发验证码的入口登录流程走不通。
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final String BEARER = "Bearer ";

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /** 下发验证码（开发环境固定 123456，见 DevSmsSender） */
    @PostMapping("/sms-code")
    public ApiResponse<Map<String, Object>> sendCode(@Valid @RequestBody SmsCodeRequest request) {
        Duration ttl = authService.sendCode(request.phone());
        return ApiResponse.ok(Map.of("expiresIn", ttl.toSeconds()));
    }

    @PostMapping("/login")
    public ApiResponse<TokenPair> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(
                request.phone(), request.code(), request.deviceId(), request.platform()));
    }

    @PostMapping("/refresh")
    public ApiResponse<TokenPair> refresh(@Valid @RequestBody RefreshRequest request) {
        return ApiResponse.ok(authService.refresh(request.refreshToken(), request.deviceId()));
    }

    /** 登出幂等：无论令牌是否有效都返回 204 */
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        if (authorization == null || !authorization.startsWith(BEARER)) {
            throw new BizException(ErrorCodes.UNAUTHENTICATED, 401, "缺少 Bearer 令牌");
        }
        String token = authorization.substring(BEARER.length()).trim();
        if (token.isEmpty()) {
            throw new BizException(ErrorCodes.UNAUTHENTICATED, 401, "令牌为空");
        }
        authService.logout(token);
    }

    /** 当前身份。注意 lvl 取自令牌，可能滞后于库中授权级别（5.7.3 第 186 行） */
    @GetMapping("/me")
    public ApiResponse<MeResponse> me(@AuthenticationPrincipal AuthPrincipal principal) {
        return ApiResponse.ok(new MeResponse(
                principal.userId().toString(),
                principal.deviceId(),
                principal.lvl()));
    }
}
