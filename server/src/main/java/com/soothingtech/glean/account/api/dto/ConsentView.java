package com.soothingtech.glean.account.api.dto;

import java.time.OffsetDateTime;
import java.util.Map;

/** GET /consent：当前授权级别与范围 */
public record ConsentView(
        String level,
        Map<String, Object> scope,
        OffsetDateTime grantedAt,
        OffsetDateTime revokedAt,
        OffsetDateTime updatedAt) {
}
