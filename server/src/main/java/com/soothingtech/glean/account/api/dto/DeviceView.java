package com.soothingtech.glean.account.api.dto;

import java.time.OffsetDateTime;

/** GET /devices 列表项 */
public record DeviceView(
        String id,
        String platform,
        OffsetDateTime lastSeenAt,
        OffsetDateTime revokedAt) {
}
