package com.soothingtech.glean.account.api.dto;

import com.soothingtech.glean.account.domain.ConsentLevel;

import java.util.Map;

/**
 * PUT /consent 请求。scope 与 consent.scope 的 jsonb 同构：
 * notifyApps / qqGroups / quietHours。
 * clientRequestId 用于幂等（与 Idempotency-Key 请求头等价，二者取其一即可）。
 */
public record UpdateConsentRequest(
        String level,
        Map<String, Object> scope,
        String clientRequestId) {

    public ConsentLevel toLevel() {
        return ConsentLevel.parse(level);
    }
}
