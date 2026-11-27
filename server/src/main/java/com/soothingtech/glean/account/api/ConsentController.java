package com.soothingtech.glean.account.api;

import com.soothingtech.glean.account.api.dto.ConsentUpdateResult;
import com.soothingtech.glean.account.api.dto.ConsentView;
import com.soothingtech.glean.account.api.dto.UpdateConsentRequest;
import com.soothingtech.glean.account.app.ConsentService;
import com.soothingtech.glean.account.token.AuthPrincipal;
import com.soothingtech.glean.common.ApiResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 授权级别与范围（5.7.3.2 接口表第 132—133 行）。
 * PUT 幂等：Idempotency-Key 请求头与请求体 clientRequestId 等价。
 */
@RestController
@RequestMapping("/api/v1/consent")
public class ConsentController {

    private final ConsentService consentService;

    public ConsentController(ConsentService consentService) {
        this.consentService = consentService;
    }

    @GetMapping
    public ApiResponse<ConsentView> current(@AuthenticationPrincipal AuthPrincipal principal) {
        return ApiResponse.ok(consentService.view(principal.userId()));
    }

    @PutMapping
    public ApiResponse<ConsentUpdateResult> update(
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody UpdateConsentRequest request) {

        String key = idempotencyKey != null && !idempotencyKey.isBlank()
                ? idempotencyKey
                : request.clientRequestId();

        ConsentUpdateResult result = consentService.update(
                principal.userId(), request.toLevel(), request.scope(), key);
        return ApiResponse.ok(result);
    }
}
