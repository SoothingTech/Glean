package com.soothingtech.glean.account.api.dto;

import java.time.OffsetDateTime;
import java.util.Map;

/** 审计记录条目 */
public record AuditItemView(
        String id,
        String action,
        Map<String, Object> payload,
        OffsetDateTime createdAt) {
}
